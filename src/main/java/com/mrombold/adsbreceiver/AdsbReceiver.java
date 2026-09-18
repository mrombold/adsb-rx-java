package com.mrombold.adsbreceiver;

import com.mrombold.adsbreceiver.adsb1090.Adsb1090Client;
import com.mrombold.adsbreceiver.adsb1090.Adsb1090Parser;
import com.mrombold.adsbreceiver.adsb978.Adsb978TrafficClient;
import com.mrombold.adsbreceiver.adsb978.Adsb978WeatherClient;
import com.mrombold.adsbreceiver.ahrs.AhrsService;
import com.mrombold.adsbreceiver.ahrs.AhrsUnixClient;
import com.mrombold.adsbreceiver.gdl90.Gdl90Encoder;
import com.mrombold.adsbreceiver.gdl90.Gdl90Service;
import com.mrombold.adsbreceiver.gdl90.Gdl90Transmitter;
import com.mrombold.adsbreceiver.gdl90.Gdl90UdpPublisher;
import com.mrombold.adsbreceiver.gps.GpsdClient;
import com.mrombold.adsbreceiver.position.PositionService;
import com.mrombold.adsbreceiver.traffic.TrafficService;
import com.mrombold.adsbreceiver.weather.WeatherService;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class AdsbReceiver {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Java ADS-B Receiver");
        System.out.println("==========================");
        /*
         * Domain/state services.
         */
        TrafficService trafficService = new TrafficService();
        PositionService positionService = new PositionService();
        WeatherService weatherService = new WeatherService();
        AhrsService ahrsService = new AhrsService();

        /*
         * Protocol parsers / clients.
         */
        Adsb1090Parser adsb1090Parser = new Adsb1090Parser();
        Adsb1090Client adsb1090 = new Adsb1090Client("127.0.0.1",30003, adsb1090Parser, trafficService);
        Adsb978TrafficClient adsb978Traffic = new Adsb978TrafficClient("127.0.0.1",30979, trafficService);
        Adsb978WeatherClient adsb978Weather = new Adsb978WeatherClient("127.0.0.1",30978, weatherService);
        GpsdClient gpsd = new GpsdClient("127.0.0.1",2947, positionService);
        AhrsUnixClient ahrs = new AhrsUnixClient(Path.of("/tmp/ahrs.sock"), ahrsService);

        /*
         * GDL90 output.
         */
        Gdl90Encoder encoder = new Gdl90Encoder();

        try (
                Gdl90UdpPublisher publisher = new Gdl90UdpPublisher("192.168.10.255",4000);
                var executor = Executors.newVirtualThreadPerTaskExecutor()
        ) {
            Gdl90Transmitter transmitter = new Gdl90Transmitter(publisher);

            Gdl90Service gdl90 = new Gdl90Service(encoder, transmitter, trafficService, positionService, weatherService, ahrsService);
            executor.submit(transmitter::run);
            /*
             * One logical activity
             * per virtual thread.
             */
            executor.submit(trafficService::run);
            executor.submit(adsb1090::run);
            executor.submit(adsb978Traffic::run);
            executor.submit(adsb978Weather::run);
            executor.submit(gpsd::run);
            executor.submit(gdl90::runStandardLoop);
            executor.submit(gdl90::runWeatherLoop);

            /*
             * Leave AHRS off until the
             * rest is proven.
             */
            // executor.submit(ahrs::run);
            // executor.submit(gdl90::runAhrsLoop);

            new CountDownLatch(1).await();
        }
    }
}