package com.redhat.rhjmc.containerjfr.reports;

import java.io.InputStream;
import java.util.Objects;

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
        if (args.length != 2) {
            System.out.println("Expected two arguments: host connection string and recording name");
        }
        String hostString = args[0];
        String recordingName = args[1];

        JMCConnectionToolkit ctk = new JMCConnectionToolkit(new ClientWriterImpl(), new Clock());

        JMCConnection connection = null;
        try {
            connection = ctk.connect(hostString);
            IFlightRecorderService service = connection.getService();
            IRecordingDescriptor recording = getDescriptorByName(service, recordingName);
            try (InputStream stream = service.openStream(recording, false)) {
                String report = JfrHtmlRulesReport.createReport(stream);
                System.out.println(report);
            }
        } catch (Exception e) {
            System.out.println("Unexpected exception, quitting...");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
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
            System.out.print("\t[LOG] " + s);
        }
    }
}
