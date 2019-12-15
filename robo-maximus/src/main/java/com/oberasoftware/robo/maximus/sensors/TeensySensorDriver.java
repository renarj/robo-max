package com.oberasoftware.robo.maximus.sensors;

import com.oberasoftware.base.event.impl.LocalEventBus;
import com.oberasoftware.robo.api.Robot;
import com.oberasoftware.robo.api.exceptions.RoboException;
import com.oberasoftware.robo.api.sensors.SensorDriver;
import com.oberasoftware.robo.maximus.TeensyProxySerialConnector;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class TeensySensorDriver implements SensorDriver<TeensyPort> {
    private static final Logger LOG = getLogger(TeensySensorDriver.class);

    private static final int CHECK_INTERVAL = 1000;

    static final String INA_260_CURRENT = "ina260.current";
    static final String INA_260_VOLTAGE = "ina260.voltage";
    static final String INA_260_POWER = "ina260.power";
    static final String LSM_9_DS_1_ROLL = "LSM9DS1.roll";
    static final String LSM_9_DS_1_PITCH = "LSM9DS1.pitch";
    static final String LSM_9_DS_1_HEADING = "LSM9DS1.heading";

    @Autowired
    private TeensyProxySerialConnector proxySerialConnector;

    @Autowired
    private LocalEventBus eventBus;

    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    private List<TeensyPort> ports = new ArrayList<>();

    @Override
    public List<TeensyPort> getPorts() {
        return newArrayList(ports);
    }

    @Override
    public TeensyPort getPort(String portId) {
        return ports.stream().filter(p -> p.getName().equalsIgnoreCase(portId))
                .findFirst()
                .orElseThrow(() -> new RoboException("Could not find sensor port: " + portId));
    }

    @Override
    public void activate(Robot robot, Map<String, String> properties) {
        ports.add(new TeensyPort(LSM_9_DS_1_ROLL));
        ports.add(new TeensyPort(LSM_9_DS_1_PITCH));
        ports.add(new TeensyPort(LSM_9_DS_1_HEADING));
        ports.add(new TeensyPort(INA_260_CURRENT));
        ports.add(new TeensyPort(INA_260_VOLTAGE));
        ports.add(new TeensyPort(INA_260_POWER));

        executorService.submit(() -> {
            while(!Thread.currentThread().isInterrupted()) {
                byte[] d = proxySerialConnector.sendAndReceiveCommand("sensors", new HashMap<>(), true);
                notifyPorts(new String(d));

                sleepUninterruptibly(CHECK_INTERVAL, MILLISECONDS);
            }
        });
    }

    private void notifyPorts(String json) {
        JSONObject jo = new JSONObject(json);
        JSONObject sensors = jo.getJSONObject("sensors");
        for(String key: sensors.keySet()) {
            JSONObject sensor = sensors.getJSONObject(key);
            LOG.debug("Sensor: {} data: {}", key, sensor.toString());

            for(String attribute : sensor.keySet()) {
                LOG.debug("Sensor data: {} value: {}", key + "." + attribute, sensor.get(attribute));

                String sensorId = key + "." + attribute;
                double value = sensor.getDouble(attribute);
                getPort(sensorId).notify(value);


            }
        }
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }
}