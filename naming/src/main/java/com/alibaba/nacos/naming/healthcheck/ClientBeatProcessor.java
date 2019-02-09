/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.nacos.naming.healthcheck;


import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.nacos.naming.boot.SpringContext;
import com.alibaba.nacos.naming.core.*;
import com.alibaba.nacos.naming.misc.Loggers;
import com.alibaba.nacos.naming.misc.UtilsAndCommons;
import com.alibaba.nacos.naming.push.PushService;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author nkorange
 */
public class ClientBeatProcessor implements Runnable {
    public static final long CLIENT_BEAT_TIMEOUT = TimeUnit.SECONDS.toMillis(15);
    private RsInfo rsInfo;
    private Service service;

    @JSONField(serialize = false)
    public PushService getPushService() {
        return SpringContext.getAppContext().getBean(PushService.class);
    }

    public RsInfo getRsInfo() {
        return rsInfo;
    }

    public void setRsInfo(RsInfo rsInfo) {
        this.rsInfo = rsInfo;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public String getType() {
        return "CLIENT_BEAT";
    }

    public void process() {
        Service service = this.service;
        Loggers.EVT_LOG.debug("[CLIENT-BEAT] processing beat: {}", rsInfo.toString());

        String ip = rsInfo.getIp();
        String clusterName = rsInfo.getCluster();
        int port = rsInfo.getPort();
        Cluster cluster = service.getClusterMap().get(clusterName);
        List<Instance> instances = cluster.allIPs(true);

        for (Instance instance : instances) {
            if (instance.getIp().equals(ip) && instance.getPort() == port) {
                Loggers.EVT_LOG.debug("[CLIENT-BEAT] refresh beat: {}", rsInfo.toString());
                instance.setLastBeat(System.currentTimeMillis());
                if (!instance.isMarked()) {
                    if (!instance.isValid()) {
                        instance.setValid(true);
                        Loggers.EVT_LOG.info("dom: {} {POS} {IP-ENABLED} valid: {}:{}@{}, region: {}, msg: client beat ok",
                            cluster.getDom().getName(), ip, port, cluster.getName(), UtilsAndCommons.LOCALHOST_SITE);
                        getPushService().serviceChanged(service.getNamespaceId(), this.service.getName());
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        process();
    }
}
