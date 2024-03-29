package com.redhat.rhjmc.containerjfr.reports;

import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.redhat.rhjmc.containerjfr.core.ContainerJfrCore;
import com.redhat.rhjmc.containerjfr.core.net.JMCConnection;
import com.redhat.rhjmc.containerjfr.core.net.JMCConnectionToolkit;
import com.redhat.rhjmc.containerjfr.core.sys.Clock;
import com.redhat.rhjmc.containerjfr.core.tui.ClientWriter;

import org.openjdk.jmc.flightrecorder.rules.report.html.JfrHtmlRulesReport;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;

public class Generator {
    public static void main(String[] args) {
        //TODO validations
        ClientWriter cw = new ClientWriterImpl();
        if (args.length != 3) {
            cw.println("Expected three arguments: hostname, port, and recording name");
            System.exit(1);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> cw.println("Terminating")));
        try {
            ContainerJfrCore.initialize();
        } catch (Exception e) {
            cw.println(e);
            System.exit(1);
        }
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);
        String recordingName = args[2];
        cw.println(String.format("Analyzing %s from %s:%d...", recordingName, hostname, port));

        JMCConnectionToolkit ctk = new JMCConnectionToolkit(cw, new Clock());

        cw.println(String.format("Attempting connection to %s:%d", hostname, port));
        try (JMCConnection connection = ctk.connect(hostname, port)){
            cw.println("Connection established");
            IFlightRecorderService service = connection.getService();
            IRecordingDescriptor recording = getDescriptorByName(service, recordingName);
            if (recording == null) {
                cw.println("Recording not found");
                System.exit(2);
            }
            cw.println("Recording found");
            try (InputStream stream = service.openStream(recording, false)) {
                cw.print("Processing ");
                ScheduledFuture<?> progress = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> System.err.print(". "), 0, 1, TimeUnit.SECONDS);
                System.out.println(JfrHtmlRulesReport.createReport(stream));
                progress.cancel(false);
                System.exit(0);
            }
        } catch (Exception e) {
            cw.println(e);
            cw.println("Unexpected exception, quitting...");
            System.exit(3);
        }
    }

    private static IRecordingDescriptor getDescriptorByName(IFlightRecorderService service, String name) throws Exception {
        for (IRecordingDescriptor descriptor : service.getAvailableRecordings()) {
            if (Objects.equals(descriptor.getName(), name)) {
                return descriptor;
            }
        }
        return null;
    }

    private static class ClientWriterImpl implements ClientWriter {
        @Override
        public void print(String s) {
            System.err.print("[LOG] " + s);
        }
    }
}
