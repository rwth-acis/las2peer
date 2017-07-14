package i5.las2peer.restMapper;

public class ResponseEntity {

    private int code;
    private String msg;

    public ResponseEntity(int code) {
        this(code, null);
    }

    public ResponseEntity(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

}
