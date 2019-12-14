package net.pregi.networking.speedtest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkTesting {

    private static Charset ISO_8859_1 = Charset.forName("ISO_8859_1");
    private static final byte[] ENDOFHEADER_BYTES = "\r\n".getBytes(ISO_8859_1);
    private static final Pattern PATTERN_RESPONSE_STATUSLINE = Pattern.compile("^(\\S+)\\s(\\S+)\\s(.+)$");
    private static final Pattern PATTERN_RESPONSE_HEADER = Pattern.compile("^([\\w\\-]+):\\s*(.+)\\s*$");

    //private static final int PATTERNGROUP_STATUSLINE_HTTPVER = 1;
    private static final int PATTERNGROUP_STATUSLINE_STATUS = 2;
    //private static final int PATTERNGROUP_STATUSLINE_REASON = 3;
    private static final int PATTERNGROUP_HEADER_NAME = 1;
    private static final int PATTERNGROUP_HEADER_VALUE = 2;

    private static class ResponseReadState {
        boolean previouslyCR = false;
        boolean hasEndedLine = true;
        boolean receivedFirstLine = false;
        StringBuffer currentLine = new StringBuffer();

        long contentLength = 0;
        boolean isOk = false;
    }

    private static TransferMeasure readResponseBody(InputStream responseStream, long contentLength, OnIOProgressListener listener) throws IOException {
        if (contentLength>0) {
            //byte[] devNull = new byte[0x100000]; // 1048576, 1 MB buffer. Should be accommodating enough, right?
            byte[] devNull = new byte[0x40000]; // 262144, 1/4 MB buffer.
            long downloadedBytes = 0;

            if (listener != null) {
                listener.onIOProgress(0, contentLength, IOProcessMode.DOWNLOAD);
                long time0 = System.nanoTime();
                for (long readBytes = 0; downloadedBytes < contentLength && (readBytes = responseStream.read(devNull)) != -1;) {
                    downloadedBytes += readBytes;
                    listener.onIOProgress(downloadedBytes, contentLength, IOProcessMode.DOWNLOAD);
                }
                long time1 = System.nanoTime();
                return new TransferMeasure(time0, time1, downloadedBytes);
            } else {
                long time0 = System.nanoTime();
                for (long readBytes = 0; downloadedBytes < contentLength && (readBytes = responseStream.read(devNull)) != -1; downloadedBytes += readBytes) {
                    // since this is a time-sensitive operation,
                    // if we know listener is null
                    // then skip the cost of having to call a function every time.
                }
                long time1 = System.nanoTime();

                return new TransferMeasure(time0, time1, downloadedBytes);
            }
        } else {
            return null;
        }
    }

    /** Read a single byte.
     *
     * @return whether the read loop should be interrupted.
     * 		Output states are set to rRS.contentLength and isOk.
     * 		isOk is false if the header wasn't properly read.
     * 		isOk is also false if response is not 200.
     */
    private static boolean readResponseByte(ResponseReadState rRS, int r) {
        if (r == '\r') {
            if (rRS.previouslyCR) {
                // A previous CR did not follow a LF, so it's counted as part of the line.
                rRS.currentLine.append('\r');
                rRS.hasEndedLine = false;
            }
            rRS.previouslyCR = true;
        } else if (r == '\n') {
            if (rRS.previouslyCR) {
                // CRLF hsa just been read.
                if (rRS.hasEndedLine) {
                    // Response header has just been concluded.
                    rRS.isOk = true;
                    return true;
                } else {
                    // Inspect the line and see what the header's name and value are.
                    String receivedLine = rRS.currentLine.toString();
                    rRS.currentLine.setLength(0);

                    if (!rRS.receivedFirstLine) {
                        // receive status
                        Matcher m = PATTERN_RESPONSE_STATUSLINE.matcher(receivedLine);
                        // TODO: decide whether to keep the response examination code.
                        // 		We don't really need it to measure upload times.
                        //		However, we might be interested in seeing what the response content is, anyway.
                        // At this point, since has upload transit has already been measured,
                        //		we can return a TransferMeasure instead of null where doDownload would have.
                        if (m.find()) {
                            if (!"200".equals(m.group(PATTERNGROUP_STATUSLINE_STATUS))) {
                                System.err.println("Received response not OK: "+receivedLine);
                                rRS.isOk = false;
                                return true;
                            }
                        } else {
                            return true;
                        }
                        rRS.receivedFirstLine = true;
                    } else {
                        // receive header fields.
                        Matcher m = PATTERN_RESPONSE_HEADER.matcher(receivedLine);
                        if (m.find()) {
                            String headerName = m.group(PATTERNGROUP_HEADER_NAME);
                            String value = m.group(PATTERNGROUP_HEADER_VALUE);

                            if ("Content-Length".equalsIgnoreCase(headerName)) {
                                rRS.contentLength = Long.parseLong(value);
                            }
                        } else {
                            System.err.println("response header line not understood: "+receivedLine);
                            rRS.isOk = false;
                            return true;
                        }
                    }
                }

                rRS.previouslyCR = false;
                rRS.hasEndedLine = true;
            } else {
                // It's not a LF that followed a CR, so it's added like a normal character.
                rRS.currentLine.append('\n');
                rRS.previouslyCR = false;

                rRS.hasEndedLine = false;
            }
        } else {
            // This is a character that appears in whatever line it is in.
            // It isn't something that could signal the line's, or header's, end.
            if (rRS.previouslyCR) {
                // If it isn't followed by a LF, it's just a normal character.
                rRS.currentLine.append('\r');
            }
            rRS.currentLine.append((char)r);

            rRS.previouslyCR = false;
            rRS.hasEndedLine = false;
        }

        // Tell the loop to keep going.
        return false;
    }

    public static TransferMeasure doHTTPPing(Socket s, byte[] baseRequestHeader) throws IOException {
        // Prepare request variables and do stuff here so they don't get in the way of time measurement.
        // Because we need precise control of what bytes are transmitted
        //		so we can start stopwatching at the very moment specific bytes were sent,
        // 		we do not wrap the stream in a buffer.
        OutputStream requestStream = s.getOutputStream();

        // Prepare response variables and do stuff ahead of time so they also don't get in the way of time measurement.
        // We'll allow Java to buffer the response as we want to time the whole small thing being received.
        InputStream responseStream = s.getInputStream();
        final ResponseReadState rRS = new ResponseReadState();

        // Send request
        requestStream.write(baseRequestHeader);
        long time0 = System.nanoTime();
        long time1 = time0;
        requestStream.write(ENDOFHEADER_BYTES);
        requestStream.flush();

        // Get response
        // TODO: Apparently, ping readings are padded somehow.
        // I wonder if I need to go levels lower like in download and upload...
        boolean receivedResponse = false;
        long bytesReceived = 0;
        for (int r=0;(r=responseStream.read()) != -1;bytesReceived++) {
            if (!receivedResponse) {
                time1 = System.nanoTime();
                receivedResponse = true;
            }
            if (readResponseByte(rRS, r)) {
                break;
            }
        }

        if (rRS.isOk) {
            readResponseBody(responseStream, rRS.contentLength, null);
        }

        if (time1 != time0) {
            return new TransferMeasure(time0, time1, bytesReceived);
        } else {
            return null;
        }
    }

    public static TransferMeasure doHTTPDownload(Socket s, byte[] baseRequestHeader, OnIOProgressListener listener) throws IOException {
        // Prepare request
        byte[] requestBytes = baseRequestHeader;
        OutputStream requestStream = s.getOutputStream();

        // Send request.
        requestStream.write(requestBytes);
        requestStream.write(ENDOFHEADER_BYTES);
        requestStream.flush();

        // Get response.
        // We  can't use buffers because
        //		we don't want Java downloading the bytes ahead of time (into a buffer)
        //		before we can measure their transit speed.
        // We want Java to download exactly the bytes making up the header.
        // This way, we know ahead of time how many bytes to expect in the body (via "Content-Length")
        //		and when to start keeping time.
        InputStream responseStream = s.getInputStream();
        ResponseReadState rRS = new ResponseReadState();
        for (int r=0;(r=responseStream.read()) != -1;) {
            if (readResponseByte(rRS, r)) {
                if (!rRS.isOk) {
                    return null;
                } else {
                    break;
                }
            }
        }

        return readResponseBody(responseStream, rRS.contentLength, listener);
    }

    public static TransferMeasure doHTTPUpload(Socket s, byte[] baseRequestBytes, byte[] data, long size, OnIOProgressListener listener) throws IOException {
        OutputStream requestStream = s.getOutputStream();

        // Prepare response variables
        InputStream responseStream = s.getInputStream();
        ResponseReadState rRS = new ResponseReadState();

        // Send request.
        requestStream.write(baseRequestBytes);
        requestStream.write(("Content-Type: application/octet-stream\r\nContent-Length: "+Long.toString(size)+"\r\n").getBytes(ISO_8859_1));
        requestStream.write(ENDOFHEADER_BYTES);
        requestStream.flush();

        // Upload.
        long pendingSize = size;
        long time0 = System.nanoTime();
        long time1 = time0;
        if (listener != null) {
            listener.onIOProgress(0, size, IOProcessMode.UPLOAD);
            while (pendingSize > 0) {
                if (pendingSize >= data.length) {
                    requestStream.write(data);
                    pendingSize -= data.length;
                    listener.onIOProgress(size-pendingSize, size, IOProcessMode.UPLOAD);
                } else {
                    requestStream.write(data, 0, (int) pendingSize);
                    pendingSize = 0;
                    listener.onIOProgress(size, size, IOProcessMode.UPLOAD);
                }
            }
        } else {
            while (pendingSize > 0) {
                if (pendingSize >= data.length) {
                    requestStream.write(data);
                    pendingSize -= data.length;
                } else {
                    requestStream.write(data, 0, (int) pendingSize);
                    pendingSize = 0;
                }
            }
        }
        requestStream.flush();
        //

        // Receive response.
        // We want to stop the watch exactly as we receive the first response byte,
        //		at which point, we are sure the server has received the entire upload.
        // flush() apparently does not guarantee that data have been received by the server when it returns.
        // Therefore, we need the finer control of doDownload to get that first byte,
        //		and so has its download process copied.
        // I don't think we have to care about what the response content is;
        //		only that we get the first byte of the response, for the purposes of upload measurement.

        boolean receivedFirstByte = false;
        TransferMeasure uploadMeasure = null;
        for (int r=0;(r=responseStream.read()) != -1;) {
            if (!receivedFirstByte) {
                time1 = System.nanoTime();
                uploadMeasure = new TransferMeasure(time0, time1, size);
                receivedFirstByte = true;
            }
            if (readResponseByte(rRS, r)) {
                if (!rRS.isOk) {
                    return uploadMeasure;
                } else {
                    break;
                }
            }
        }

        readResponseBody(responseStream, rRS.contentLength, null);
        return uploadMeasure;
    }
}
