package i5.las2peer.restMapper;

import java.util.ArrayList;

public class ExceptionEntity extends ResponseEntity {

    public final ArrayList<String> stacktrace = new ArrayList<>();

    public ExceptionEntity(int code, Throwable e) {
        super(code, e.getMessage());
        stacktrace.add(e.toString());
        while ((e = e.getCause()) != null) {
            stacktrace.add(e.toString());
        }
    }

}