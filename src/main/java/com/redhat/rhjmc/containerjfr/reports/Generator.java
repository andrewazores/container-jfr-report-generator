package com.redhat.rhjmc.containerjfr.reports;

import java.io.InputStream;
import java.util.Objects;

import com.redhat.rhjmc.containerjfr.core.jmc.RegistryProvider;
import com.redhat.rhjmc.containerjfr.core.net.JMCConnection;
import com.redhat.rhjmc.containerjfr.core.net.JMCConnectionToolkit;
import com.redhat.rhjmc.containerjfr.core.sys.Clock;
import com.redhat.rhjmc.containerjfr.core.tui.ClientWriter;

import org.eclipse.core.runtime.RegistryFactory;
import org.openjdk.jmc.flightrecorder.rules.report.html.JfrHtmlRulesReport;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;

public class Generator {
    public static void main(String[] args) {
        //TODO validations
        if (args.length != 3) {
            System.err.println("Expected three arguments: hostname, port, and recording name");
        }
        ClientWriter cw = new ClientWriterImpl();
        try {
            RegistryFactory.setDefaultRegistryProvider(new RegistryProvider());
        } catch (Exception e) {
            cw.println(e);
            System.exit(1);
        }
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);
        String recordingName = args[2];
        cw.println(String.format("Analyzing %s from %s:%d...", recordingName, hostname, port));

        JMCConnectionToolkit ctk = new JMCConnectionToolkit(cw, new Clock());

        JMCConnection connection = null;
        try {
            connection = ctk.connect(hostname, port);
            IFlightRecorderService service = connection.getService();
            IRecordingDescriptor recording = getDescriptorByName(service, recordingName);
            try (InputStream stream = service.openStream(recording, false)) {
                String report = JfrHtmlRulesReport.createReport(stream);
                System.out.println(report);
            }
        } catch (Exception e) {
            cw.println(e);
            System.err.println("Unexpected exception, quitting...");
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
            System.err.print("\t[LOG] " + s);
        }
    }
}
