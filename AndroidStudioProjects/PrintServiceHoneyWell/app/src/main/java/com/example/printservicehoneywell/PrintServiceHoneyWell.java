package com.example.printservicehoneywell;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.print.PrintAttributes;
import android.print.PrinterCapabilitiesInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintJob;
import android.printservice.PrintService;
import android.printservice.PrinterDiscoverySession;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import honeywell.connection.ConnectionBase;
import honeywell.connection.Connection_Bluetooth;

public class PrintServiceHoneyWell extends PrintService {
    private final String CUSTOM_INFO = "Custom info";
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    final List<PrinterInfo> printers = new ArrayList<>();
    final List<BluetoothDevice> devices = new ArrayList<>();
    PrinterDiscoverySession session;
    ConnectionBase connectionBase = null;


    @Override
    public void onCreate() {
        super.onCreate();
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            connectionBase = null;
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if (deviceName != null && !devices.contains(device) && deviceHardwareAddress.startsWith("00:12")) {
                    devices.add(device);
                    PrinterId id = generatePrinterId(generateID());
                    PrinterInfo.Builder build = new PrinterInfo.Builder(id, deviceName, PrinterInfo.STATUS_IDLE);
                    build.setDescription(deviceHardwareAddress);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        build.setIconResourceId(R.drawable.printer);
                    }
                    printers.add(build.build());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.i(CUSTOM_INFO, "DISCOVERY STARTED");
                if (mBluetoothAdapter.startDiscovery())
                    Log.i(CUSTOM_INFO, "TRUE");
                else
                    Log.i(CUSTOM_INFO, "FALSE");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i("List", printers.toString());
                session.addPrinters(printers);
                Log.i(CUSTOM_INFO, "Devices added to list");
                try {
                    connectionBase = Connection_Bluetooth.createClient(printers.get(0).getDescription(), false);
                    Log.i(CUSTOM_INFO, "Establishing connection..");
                    //Open bluetooth socket
                    if (!connectionBase.getIsOpen()) {
                        connectionBase.open();
                    }

                    Log.d("pairDevice()", "Start Pairing...");
                    Method m = devices.get(0).getClass().getMethod("createBond", (Class[]) null);
                    m.invoke(devices.get(0), (Object[]) null);
                    Log.d("pairDevice()", "Pairing finished.");

                    /*//Sends data to printer
                    Log.i(CUSTOM_INFO, "Sending data to printer..");

                    int bytesWritten = 0;
                    int bytesToWrite = 1024;
                    int totalBytes = printData.length;
                    int remainingBytes = totalBytes;
                    while (bytesWritten < totalBytes) {
                        if (remainingBytes < bytesToWrite)
                            bytesToWrite = remainingBytes;

                        //Send data, 1024 bytes at a time until all data sent
                        conn.write(printData, bytesWritten, bytesToWrite);
                        bytesWritten += bytesToWrite;
                        remainingBytes = remainingBytes - bytesToWrite;
                        Thread.sleep(100);
                    }*/

                    //signals to close connection
                    connectionBase.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.i(CUSTOM_INFO, "DISCOVERY FINISHED");

            }
        }
    };

    @Override
    protected PrinterDiscoverySession onCreatePrinterDiscoverySession() {
        Log.d("myprinter", "MyPrintService#onCreatePrinterDiscoverySession() called");
        session = new PrinterDiscoverySession() {
            @Override
            public void onStartPrinterDiscovery(List<PrinterId> priorityList) {
                Log.d("myprinter", "PrinterDiscoverySession#onStartPrinterDiscovery(priorityList: " + priorityList + ") called");

                if (mBluetoothAdapter == null) {
                    Log.i(CUSTOM_INFO, "Device does not support bluetooth.");
                }

                // If we're already discovering, stop it
                if (mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }

                // Request discover from BluetoothAdapter
                mBluetoothAdapter.startDiscovery();

                if ((mBluetoothAdapter != null) && (mBluetoothAdapter.startDiscovery())) {
                    // Register for broadcasts when a device is discovered.
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    filter.addAction(BluetoothDevice.EXTRA_UUID);
                    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                    Log.i(CUSTOM_INFO, "Starting Discovery");
                    registerReceiver(receiver, filter);
                }

                /*for (BluetoothDevice b : pairedDevices) {
                    final PrinterId id = generatePrinterId("aaa");
                    final PrinterInfo.Builder build = new PrinterInfo.Builder(id, b.getName(), PrinterInfo.STATUS_IDLE);
                    printers.add(build.build());
                }*/

                /*PrinterCapabilitiesInfo.Builder capBuilder = new PrinterCapabilitiesInfo.Builder(printerId);
                capBuilder.addMediaSize(PrintAttributes.MediaSize.ISO_A4, true);
                capBuilder.addMediaSize(PrintAttributes.MediaSize.ISO_A3, false);
                capBuilder.addResolution(new PrintAttributes.Resolution("resolutionId", "default resolution", 600, 600), true);
                capBuilder.setColorModes(PrintAttributes.COLOR_MODE_COLOR | PrintAttributes.COLOR_MODE_MONOCHROME, PrintAttributes.COLOR_MODE_COLOR);
                builder.setCapabilities(capBuilder.build());
                printers.add(builder.build());*/
            }


            @Override
            public void onStopPrinterDiscovery() {
                Log.d("myprinter", "MyPrintService#onStopPrinterDiscovery() called");
                // Don't forget to unregister the ACTION_FOUND receiver.
                unregisterReceiver(receiver);
            }


            @Override
            public void onValidatePrinters(List<PrinterId> printerIds) {
                Log.d("myprinter", "MyPrintService#onValidatePrinters(printerIds: " + printerIds + ") called");

            }

            @Override
            public void onStartPrinterStateTracking(PrinterId printerId) {
                Log.d("myprinter", "MyPrintService#onStartPrinterStateTracking(printerId: " + printerId + ") called");
                /*PrinterCapabilitiesInfo.Builder capBuilder = new PrinterCapabilitiesInfo.Builder(printerId);
                capBuilder.addMediaSize(PrintAttributes.MediaSize., true);
                capBuilder.addMediaSize(PrintAttributes.MediaSize.ISO_A3, false);
                capBuilder.addResolution(new PrintAttributes.Resolution("resolutionId", "default resolution", 600, 600), true);
                capBuilder.setColorModes(PrintAttributes.COLOR_MODE_COLOR | PrintAttributes.COLOR_MODE_MONOCHROME, PrintAttributes.COLOR_MODE_COLOR);
                builder.setCapabilities(capBuilder.build());
                printers.add(builder.build());*/

            }

            @Override
            public void onStopPrinterStateTracking(PrinterId printerId) {
                Log.d("myprinter", "MyPrintService#onStopPrinterStateTracking(printerId: " + printerId + ") called");
            }

            @Override
            public void onDestroy() {
                Log.d("myprinter", "MyPrintService#onDestroy() called");
            }

        };
        return session;
    }

    @Override
    protected void onPrintJobQueued(PrintJob printJob) {
        Log.d("myprinter", "queued: " + printJob.getId().toString());

        /*printJob.start();

        final PrintDocument document = printJob.getDocument();

        final FileInputStream in = new FileInputStream(document.getData().getFileDescriptor());
        try {
            final byte[] buffer = new byte[4];
            @SuppressWarnings("unused") final int read = in.read(buffer);
            Log.d("myprinter", "first " + buffer.length + "bytes of content: " + toString(buffer));
        } catch (IOException e) {
            Log.d("myprinter", "", e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                assert true;
            }
        }
        printJob.complete();*/
    }

    private static String toString(byte[] bytes) {
        final StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(Byte.toString(b)).append(',');
        }
        if (sb.length() != 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    @Override
    protected void onRequestCancelPrintJob(PrintJob printJob) {
        Log.d("myprinter", "canceled: " + printJob.getId().toString());

        printJob.cancel();
    }

    private String generateID() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}
