/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.ozone.client.checksum;

import org.apache.hadoop.fs.PathIOException;
import org.apache.hadoop.hdds.client.BlockID;
import org.apache.hadoop.hdds.client.ReplicationConfig;
import org.apache.hadoop.hdds.client.StandaloneReplicationConfig;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos;
import org.apache.hadoop.hdds.protocol.proto.HddsProtos;
import org.apache.hadoop.hdds.scm.XceiverClientSpi;
import org.apache.hadoop.hdds.scm.pipeline.Pipeline;
import org.apache.hadoop.hdds.scm.storage.ContainerProtocolCalls;
import org.apache.hadoop.hdds.security.token.OzoneBlockTokenIdentifier;
import org.apache.hadoop.io.MD5Hash;
import org.apache.hadoop.ozone.client.OzoneBucket;
import org.apache.hadoop.ozone.client.OzoneVolume;
import org.apache.hadoop.ozone.client.rpc.RpcClient;
import org.apache.hadoop.ozone.om.helpers.OmKeyLocationInfo;
import org.apache.hadoop.security.token.Token;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * The helper class to compute file checksum for replicated files.
 */
public class ReplicatedFileChecksumHelper extends BaseFileChecksumHelper {
  private int blockIdx;

  ReplicatedFileChecksumHelper(
      OzoneVolume volume, OzoneBucket bucket, String keyName, long length,
      RpcClient rpcClient) throws IOException {
    super(volume, bucket, keyName, length, rpcClient);
  }

  @Override
  protected void checksumBlocks() throws IOException {
    long currentLength = 0;
    for (blockIdx = 0;
         blockIdx < getKeyLocationInfoList().size() && getRemaining() >= 0;
         blockIdx++) {
      OmKeyLocationInfo keyLocationInfo =
          getKeyLocationInfoList().get(blockIdx);
      currentLength += keyLocationInfo.getLength();
      if (currentLength > getLength()) {
        return;
      }

      if (!checksumBlock(keyLocationInfo)) {
        throw new PathIOException(
            getSrc(), "Fail to get block MD5 for " + keyLocationInfo);
      }
    }
  }

  /**
   * Return true when sounds good to continue or retry, false when severe
   * condition or totally failed.
   */
  private boolean checksumBlock(OmKeyLocationInfo keyLocationInfo)
      throws IOException {

    long blockNumBytes = keyLocationInfo.getLength();

    if (getRemaining() < blockNumBytes) {
      blockNumBytes = getRemaining();
    }
    setRemaining(getRemaining() - blockNumBytes);
    // for each block, send request
    List<ContainerProtos.ChunkInfo> chunkInfos =
        getChunkInfos(keyLocationInfo);
    ContainerProtos.ChecksumData checksumData =
        chunkInfos.get(0).getChecksumData();
    int bytesPerChecksum = checksumData.getBytesPerChecksum();
    setBytesPerCRC(bytesPerChecksum);

    ByteBuffer blockChecksumByteBuffer = getBlockChecksumFromChunkChecksums(
        keyLocationInfo, chunkInfos);
    String blockChecksumForDebug =
        populateBlockChecksumBuf(blockChecksumByteBuffer);

    LOG.debug("got reply from pipeline {} for block {}: blockChecksum={}, " +
            "blockChecksumType={}",
        keyLocationInfo.getPipeline(), keyLocationInfo.getBlockID(),
        blockChecksumForDebug, checksumData.getType());
    return true;
  }

  // copied from BlockInputStream
  /**
   * Send RPC call to get the block info from the container.
   * @return List of chunks in this block.
   */
  protected List<ContainerProtos.ChunkInfo> getChunkInfos(
      OmKeyLocationInfo keyLocationInfo) throws IOException {
    // irrespective of the container state, we will always read via Standalone
    // protocol.
    Token<OzoneBlockTokenIdentifier> token = keyLocationInfo.getToken();
    Pipeline pipeline = keyLocationInfo.getPipeline();
    BlockID blockID = keyLocationInfo.getBlockID();
    if (pipeline.getType() != HddsProtos.ReplicationType.STAND_ALONE) {
      pipeline = Pipeline.newBuilder(pipeline)
          .setReplicationConfig(new StandaloneReplicationConfig(
              ReplicationConfig
                  .getLegacyFactor(pipeline.getReplicationConfig())))
          .build();
    }

    boolean success = false;
    List<ContainerProtos.ChunkInfo> chunks;
    XceiverClientSpi xceiverClientSpi = null;
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Initializing BlockInputStream for get key to access {}",
            blockID.getContainerID());
      }
      xceiverClientSpi =
          getXceiverClientFactory().acquireClientForReadData(pipeline);

      ContainerProtos.DatanodeBlockID datanodeBlockID = blockID
          .getDatanodeBlockIDProtobuf();
      ContainerProtos.GetBlockResponseProto response = ContainerProtocolCalls
          .getBlock(xceiverClientSpi, datanodeBlockID, token);

      chunks = response.getBlockData().getChunksList();
      success = true;
    } finally {
      if (!success && xceiverClientSpi != null) {
        getXceiverClientFactory().releaseClientForReadData(
            xceiverClientSpi, false);
      }
    }

    return chunks;
  }

  // TODO: copy BlockChecksumHelper here
  ByteBuffer getBlockChecksumFromChunkChecksums(
      OmKeyLocationInfo keyLocationInfo,
      List<ContainerProtos.ChunkInfo> chunkInfoList)
      throws IOException {
    AbstractBlockChecksumComputer blockChecksumComputer =
        new ReplicatedBlockChecksumComputer(chunkInfoList);
    // TODO: support composite CRC
    blockChecksumComputer.compute();

    return blockChecksumComputer.getOutByteBuffer();
  }

  /**
   * Parses out the raw blockChecksum bytes from {@code checksumData} byte
   * buffer according to the blockChecksumType and populates the cumulative
   * blockChecksumBuf with it.
   *
   * @return a debug-string representation of the parsed checksum if
   *     debug is enabled, otherwise null.
   */
  String populateBlockChecksumBuf(ByteBuffer checksumData)
      throws IOException {
    String blockChecksumForDebug = null;
    //read md5
    final MD5Hash md5 = new MD5Hash(checksumData.array());
    md5.write(getBlockChecksumBuf());
    if (LOG.isDebugEnabled()) {
      blockChecksumForDebug = md5.toString();
    }

    return blockChecksumForDebug;
  }
}
