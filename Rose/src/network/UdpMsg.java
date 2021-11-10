package network;

import java.nio.ByteBuffer;

public class UdpMsg {
    public static final int MAX_NUM_INPUTS = 128;
    static final int MAX_COMPRESSED_BITS = 4096;
    public int sequence;
    public int magic;
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
        Invalid, ConnectReq, ConnectReply, SyncRequest, SyncReply,
        StartReq, StartRep, // <-- might remove...
        Input, QualityReport, QualityReply
    }

    // Constructor for incoming messages
    public UdpMsg(ByteBuffer buffer) {
        data = buffer.array();
        sequence =  ((data[0] & 0xFF) << 24) |
                    ((data[1] & 0xFF) << 16) |
                    ((data[2] & 0xFF) << 8 ) |
                    ((data[3] & 0xFF));
        magic = ((data[4] & 0xFF) << 24) |
                ((data[5] & 0xFF) << 16) |
                ((data[6] & 0xFF) << 8 ) |
                ((data[7] & 0xFF));
        int type =  ((data[8] & 0xFF) << 24) |
                    ((data[9] & 0xFF) << 16) |
                    ((data[10] & 0xFF) << 8 ) |
                    ((data[11] & 0xFF));
        msgType =  MsgType.values()[type];

        hdr = new Header();
        hdr.magicNumber = magic;
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
                        ((data[12] & 0xFF) << 24) |
                        ((data[13] & 0xFF) << 16) |
                        ((data[14] & 0xFF) << 8 ) |
                        ((data[15] & 0xFF));
                }
                break;
            // Connect Reply
            case 2:
                if(payload.connRep != null) {
                    payload.connRep.playerNumber =
                        ((data[12] & 0xFF) << 24) |
                        ((data[13] & 0xFF) << 16) |
                        ((data[14] & 0xFF) << 8 ) |
                        ((data[15] & 0xFF));
                    payload.connRep.random_reply =
                        ((data[16] & 0xFF) << 24) |
                        ((data[17] & 0xFF) << 16) |
                        ((data[18] & 0xFF) << 8 ) |
                        ((data[19] & 0xFF));
                }
                break;
            case 3:
                if(payload.syncReq != null) {
                    payload.syncReq.random_request =
                        ((data[12] & 0xFF) << 24) |
                        ((data[13] & 0xFF) << 16) |
                        ((data[14] & 0xFF) << 8 ) |
                        ((data[15] & 0xFF));
                    payload.syncReq.remote_magic =
                        ((data[16] & 0xFF) << 24) |
                        ((data[17] & 0xFF) << 16) |
                        ((data[18] & 0xFF) << 8 ) |
                        ((data[19] & 0xFF));
                    payload.syncReq.remote_endpoint = data[16];
                }
                break;
            case 4:
                if(payload.syncRep != null) {
                    payload.syncRep.random_reply =
                        ((data[12] & 0xFF) << 24) |
                        ((data[13] & 0xFF) << 16) |
                        ((data[14] & 0xFF) << 8) |
                        ((data[15] & 0xFF));
                }
                break;
            // StartReq
            case 5:
                if(payload.startReq != null) {
                    payload.startReq.canStart =
                        ((data[12] & 0xFF) << 24) |
                        ((data[13] & 0xFF) << 16) |
                        ((data[14] & 0xFF) << 8) |
                        ((data[15] & 0xFF));
                }
                break;
            // StartRep
            case 6:
                if(payload.startRep != null) {
                    payload.startRep.response =
                        ((data[12] & 0xFF) << 24) |
                        ((data[13] & 0xFF) << 16) |
                        ((data[14] & 0xFF) << 8) |
                        ((data[15] & 0xFF));
                }
                break;
            // Input
            case 7:
                if(payload.input != null) {
                    if(payload.input.connect_status != null) {
                        payload.input.connect_status.disconnected = data[12] == 1;
                        payload.input.connect_status.last_frame =
                            ((data[13] & 0xFF) << 24) |
                            ((data[14] & 0xFF) << 16) |
                            ((data[15] & 0xFF) << 8) |
                            ((data[16] & 0xFF));
                    } else {
                        payload.input.connect_status = new ConnectStatus();
                        payload.input.connect_status.last_frame = 0;
                        payload.input.connect_status.disconnected = false;
                    }
                    payload.input.num_inputs =
                        ((data[17] & 0xFF) << 24) |
                        ((data[18] & 0xFF) << 16) |
                        ((data[19] & 0xFF) << 8) |
                        ((data[20] & 0xFF));
                    payload.input.disconnect_requested = data[21] == 1;
                    payload.input.ack_frame =
                        ((data[22] & 0xFF) << 24) |
                        ((data[23] & 0xFF) << 16) |
                        ((data[24] & 0xFF) << 8) |
                        ((data[25] & 0xFF));
                    payload.input.start_frame =
                        ((data[26] & 0xFF) << 24) |
                        ((data[27] & 0xFF) << 16) |
                        ((data[28] & 0xFF) << 8) |
                        ((data[29] & 0xFF));
                    payload.input.inputs = new int[payload.input.num_inputs];
                    int offset = 30;
                    for(int i = 0; i < payload.input.num_inputs; i++) {
                        payload.input.inputs[i] =
                            ((data[offset] & 0xFF) << 24) |
                            ((data[offset+1] & 0xFF) << 16) |
                            ((data[offset+2] & 0xFF) << 8) |
                            ((data[offset+3] & 0xFF));
                        offset += Integer.BYTES;
                    }
                }
                break;
            case 8:
                if(payload.qualrpt != null) {
                    payload.qualrpt.ping =
                        ((long)(data[12]& 0xFF) << 54) |
                        ((long)(data[13]& 0xFF) << 48) |
                        ((long)(data[14] & 0xFF) << 40 ) |
                        ((long)(data[15] & 0xFF) << 32) |
                        ((long)(data[16] & 0xFF) << 24) |
                        ((data[17] & 0xFF) << 16) |
                        ((data[18] & 0xFF) << 8 ) |
                        ((data[19] & 0xFF));
                    payload.qualrpt.frame_advantage = data[13];
                }
                break;
            case 9:
                if(payload.qualrep != null) {
                    payload.qualrep.pong =
                        ((long)(data[12] & 0xFF) << 54) |
                        ((long)(data[13] & 0xFF) << 48) |
                        ((long)(data[14] & 0xFF) << 40 ) |
                        ((long)(data[15] & 0xFF) << 32) |
                        ((long)(data[16] & 0xFF) << 24) |
                        ((data[17] & 0xFF) << 16) |
                        ((data[18] & 0xFF) << 8 ) |
                        ((data[19] & 0xFF));
                }
        }
    }

    // Constructor for outgoing messages
    public UdpMsg(MsgType type) {
        buffer = ByteBuffer.allocate(MAX_COMPRESSED_BITS);
        hdr = new Header();
        hdr.type = type.ordinal();
        payload = new Payload(type);
    }

    public byte[] getData() {
        return data;
    }

    public ByteBuffer getBuffer() {
        buffer = ByteBuffer.allocate(MAX_COMPRESSED_BITS);
        buffer.clear();
        buffer.putInt(hdr.sequenceNumber);
        buffer.putInt(hdr.magicNumber);
        buffer.putInt(hdr.type);
        // When sending Invalid, Connect Requests, or Connect Replies, don't add any
        // extra com.rose.data to they payload for now.
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
            case 3:
                if(payload.syncReq != null) {
                    buffer.putInt(payload.syncReq.random_request);
                    buffer.putInt(payload.syncReq.remote_magic);
                    buffer.put(payload.syncReq.remote_endpoint);
                }
                break;
            case 4:
                if(payload.syncRep != null) {
                    buffer.putInt(payload.syncRep.random_reply);
                }
                break;
            // Start request
            case 5:
                if(payload.startReq != null) {
                    buffer.putInt(payload.startReq.canStart);
                }
                break;
            // Start reply
            case 6:
                if(payload.startRep != null) {
                    buffer.putInt(payload.startRep.response);
                }
                break;
            // Input
            case 7:
                if(payload.input != null) {
                    if(payload.input.connect_status != null) {
                        buffer.put(payload.input.connect_status.disconnected ? (byte) 1 : 0);
                        buffer.putInt(payload.input.connect_status.last_frame);
                    } else {
                        buffer.put((byte) 0);
                        buffer.putInt(0);
                    }

                    buffer.putInt(payload.input.num_inputs);
                    buffer.put(payload.input.disconnect_requested ? (byte) 1 : 0);
                    buffer.putInt(payload.input.ack_frame);
                    buffer.putInt(payload.input.start_frame);
                    for(int i = 0; i < payload.input.num_inputs; i++) {
                        buffer.putInt(payload.input.inputs[i]);
                    }
                }
                break;
            case 8:
                if(payload.qualrpt != null) {
                    buffer.putLong(payload.qualrpt.ping);
                    buffer.putInt(payload.qualrpt.frame_advantage);
                }
                break;
            case 9:
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
        public int magicNumber;
        public int type;
    }

    public class Payload {
        public ConnectRequest connReq;
        public ConnectReply connRep;
        public SyncRequest syncReq;
        public SyncReply syncRep;
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
                case SyncRequest:
                    syncReq = new SyncRequest();
                    break;
                case SyncReply:
                    syncRep = new SyncReply();
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

    public static class SyncRequest {
        public int random_request;
        public int remote_magic;
        public byte remote_endpoint;
    }

    public static class SyncReply {
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
        public int start_frame;
        public boolean disconnect_requested = false;
        public int ack_frame;
        public int num_inputs;
        public int[] inputs = new int[MAX_NUM_INPUTS];
    }

    public static class QualityReport {
        public long ping;
        public int frame_advantage;
    }

    public static class QualityReply {
        public long pong;
    }

	public int getPacketSize() {
		 int size = -1;
	        switch (MsgType.values()[hdr.type]) {
	            case Invalid:
	                size = 0;
	                break;
	            case ConnectReq:
	            case SyncReply:
	            case StartReq:
	            case StartRep:
	                size = 4;
	                break;
	            case ConnectReply:
	            case QualityReply:
	                size = 8;
	                break;
	            case SyncRequest:
	                size = 9;
	                break;
	            case Input:
	                size = 5+4+1+4+4+4+128;
	                break;
	            case QualityReport:
	                size = 12;
	                break;
	        }
	        assert(size > 0);
	        return size;
	}
}
