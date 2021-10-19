package network;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class UdpMsg {
    public int sequence;
    public MsgType msgType;
    public Header hdr;
    public Payload payload;
    private ByteBuffer buffer;
    private byte[] data;

    public static class ConnectStatus {
        public boolean disconnected;
        public int last_frame;
        public ConnectStatus() {}
        public ConnectStatus(boolean disconnected, int last_frame) {
            this.disconnected = disconnected;
            this.last_frame = last_frame;
        }
    }

    public enum MsgType {
        Invalid, ConnectReq, ConnectReply, StartReq, StartRep, Input,
        QualityReport, QualityReply
    }

    // Constructor for incoming messages
    public UdpMsg(ByteBuffer buffer) {
        data = buffer.array();
        sequence =  ((data[0] & 0xFF) << 24) |
                ((data[1] & 0xFF) << 16) |
                ((data[2] & 0xFF) << 8 ) |
                ((data[3] & 0xFF));
        int type =  ((data[4] & 0xFF) << 24) |
                ((data[5] & 0xFF) << 16) |
                ((data[6] & 0xFF) << 8 ) |
                ((data[7] & 0xFF));
        msgType =  MsgType.values()[type];

        hdr = new Header();
        hdr.sequenceNumber = sequence;
        hdr.type = msgType.ordinal();
        payload = new Payload(MsgType.values()[type]);

        switch(type) {
            // Invalid
            case 0:
                break;
            // Connect Request
            case 1:
                if(payload.connReq != null) {
                    payload.connReq.random_request =
                            ((data[8] & 0xFF) << 24) |
                                    ((data[9] & 0xFF) << 16) |
                                    ((data[10] & 0xFF) << 8 ) |
                                    ((data[11] & 0xFF));
                }
                break;
            // Connect Reply
            case 2:
                if(payload.connRep != null) {
                    payload.connRep.playerNumber =
                            ((data[8] & 0xFF) << 24) |
                                    ((data[9] & 0xFF) << 16) |
                                    ((data[10] & 0xFF) << 8 ) |
                                    ((data[11] & 0xFF));
                    payload.connRep.random_reply =
                            ((data[12] & 0xFF) << 24) |
                                    ((data[13] & 0xFF) << 16) |
                                    ((data[14] & 0xFF) << 8 ) |
                                    ((data[15] & 0xFF));
                }
                break;
            // StartReq
            case 3:
                if(payload.startReq != null) {
                    payload.startReq.canStart =
                            ((data[8] & 0xFF) << 24) |
                                    ((data[9] & 0xFF) << 16) |
                                    ((data[10] & 0xFF) << 8) |
                                    ((data[11] & 0xFF));
                }
                break;
            // StartRep
            case 4:
                if(payload.startRep != null) {
                    payload.startRep.response =
                            ((data[8] & 0xFF) << 24) |
                                    ((data[9] & 0xFF) << 16) |
                                    ((data[10] & 0xFF) << 8) |
                                    ((data[11] & 0xFF));
                }
                break;
            // Input
            case 5:
                if(payload.input != null) {
                    payload.input.connect_status = new ConnectStatus();
                    boolean disconnect = data[8] == 1;
                    payload.input.connect_status.disconnected = disconnect;
                    payload.input.connect_status.last_frame =
                            ((data[9] & 0xFF) << 24) |
                                    ((data[10] & 0xFF) << 16) |
                                    ((data[11] & 0xFF) << 8 ) |
                                    ((data[12] & 0xFF));
                    payload.input.frame =
                            ((data[13] & 0xFF) << 24) |
                                    ((data[14] & 0xFF) << 16) |
                                    ((data[15] & 0xFF) << 8 ) |
                                    ((data[16] & 0xFF));
                    payload.input.input =
                            ((data[17] & 0xFF) << 24) |
                                    ((data[18] & 0xFF) << 16) |
                                    ((data[19] & 0xFF) << 8 ) |
                                    ((data[20] & 0xFF));
                }
                break;
            case 6:
                if(payload.qualrpt != null) {
                    payload.qualrpt.ping =
                            ((long) (data[8] & 0xFF) << 54) |
                                    ((long)(data[9] & 0xFF) << 48) |
                                    ((long)(data[10] & 0xFF) << 40 ) |
                                    ((long)(data[11] & 0xFF) << 32) |
                                    ((long) (data[12] & 0xFF) << 24) |
                                    ((data[13] & 0xFF) << 16) |
                                    ((data[14] & 0xFF) << 8 ) |
                                    ((data[15] & 0xFF));
                    payload.qualrpt.frame_advantage = data[13];
                }
                break;
            case 7:
                if(payload.qualrep != null) {
                    payload.qualrep.pong =
                            ((long) (data[8] & 0xFF) << 54) |
                                    ((long)(data[9] & 0xFF) << 48) |
                                    ((long)(data[10] & 0xFF) << 40 ) |
                                    ((long)(data[11] & 0xFF) << 32) |
                                    ((long) (data[12] & 0xFF) << 24) |
                                    ((data[13] & 0xFF) << 16) |
                                    ((data[14] & 0xFF) << 8 ) |
                                    ((data[15] & 0xFF));
                }
        }
    }

    // Constructor for outgoing messages
    public UdpMsg(MsgType type) {
        buffer = ByteBuffer.allocate(32);
        hdr = new Header();
        hdr.type = type.ordinal();
        payload = new Payload(type);
    }

    public byte[] getData() {
        return data;
    }

    public ByteBuffer getBuffer() {
        buffer = ByteBuffer.allocate(32);
        buffer.clear();
        buffer.putInt(hdr.sequenceNumber);
        buffer.putInt(hdr.type);
        // When sending Invalid, Connect Requests, or Connect Replies, don't add any
        // extra data to they payload for now.
        switch(hdr.type) {
            // Invalid
            case 0:
                // Connect Request
            case 1:
                if(payload.connReq != null) {
                    buffer.putInt(payload.connReq.random_request);
                }
                break;
            // Connect Reply
            case 2:
                if(payload.connRep != null) {
                    buffer.putInt(payload.connRep.playerNumber);
                    buffer.putInt(payload.connRep.random_reply);
                }
                break;
            // Start request
            case 3:
                if(payload.startReq != null) {
                    buffer.putInt(payload.startReq.canStart);
                }
                break;
            // Start reply
            case 4:
                if(payload.startRep != null) {
                    buffer.putInt(payload.startRep.response);
                }
                break;
            // Input
            case 5:
                if(payload.input != null) {
                    byte disconnect = payload.input.connect_status.disconnected ?
                        (byte)1 : 0;
                    buffer.put((byte)1);
                    buffer.putInt(payload.input.connect_status.last_frame);
                    buffer.putInt(payload.input.frame);
                    buffer.putInt(payload.input.input);
                }
                break;
            case 6:
                if(payload.qualrpt != null) {
                    buffer.putLong(payload.qualrpt.ping);
                    buffer.putInt(payload.qualrpt.frame_advantage);
                }
                break;
            case 7:
                if(payload.qualrep != null) {
                    buffer.putLong(payload.qualrep.pong);
                }
                break;
        }
        buffer.flip();
        return buffer;
    }

    public static class Header {
        public int sequenceNumber;
        public int type;
    }

    public class Payload {
        public ConnectRequest connReq;
        public ConnectReply connRep;
        public StartReq startReq;
        public StartRep startRep;
        public Input input;
        public QualityReport qualrpt;
        public QualityReply qualrep;

        public Payload(MsgType type) {
            switch(type) {
                case Invalid:
                    break;
                case ConnectReq:
                    connReq = new ConnectRequest();
                    break;
                case ConnectReply:
                    connRep = new ConnectReply();
                    break;
                case StartReq:
                    startReq = new StartReq();
                    break;
                case StartRep:
                    startRep = new StartRep();
                    break;
                case Input:
                    input = new Input();
                    input.connect_status = new ConnectStatus();
                    break;
                case QualityReport:
                    qualrpt = new QualityReport();
                    break;
                case QualityReply:
                    qualrep = new QualityReply();
                    break;
            }
        }
    }

    public static class ConnectRequest {
        public int random_request;
    }

    public static class ConnectReply {
        public int playerNumber;
        public int random_reply;
    }

    public static class StartReq {
        public int canStart;
    }

    public static class StartRep {
        public int response;
    }

    public static class Input {
        public ConnectStatus connect_status;
        public int frame;
        public int input;
    }

    public static class QualityReport {
        public long ping;
        public int frame_advantage;
    }

    public static class QualityReply {
        public long pong;
    }
}
