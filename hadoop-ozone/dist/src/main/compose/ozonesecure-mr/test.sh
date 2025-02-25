#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#suite:MR

COMPOSE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
export COMPOSE_DIR

# shellcheck source=/dev/null
source "$COMPOSE_DIR/../testlib.sh"

export SECURITY_ENABLED=true

start_docker_env

execute_command_in_container rm sudo bash -c "sed -i -e 's/^mirrorlist/#&/' -e 's/^#baseurl/baseurl/' -e 's/mirror.centos.org/vault.centos.org/' /etc/yum.repos.d/*.repo"
execute_command_in_container rm sudo yum install -y krb5-workstation

execute_robot_test om kinit.robot

execute_robot_test om createmrenv.robot

# reinitialize the directories to use
export OZONE_DIR=/opt/ozone

# shellcheck source=/dev/null
source "$COMPOSE_DIR/../testlib.sh"

execute_robot_test rm kinit-hadoop.robot

for scheme in o3fs ofs; do
  execute_robot_test rm -v "SCHEME:${scheme}" -N "hadoopfs-${scheme}" ozonefs/hadoopo3fs.robot
  execute_robot_test rm -v "SCHEME:${scheme}" -N "mapreduce-${scheme}" mapreduce.robot
done

stop_docker_env

generate_report
