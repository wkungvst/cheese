package exceptions;

/**
 * Created by wkung on 12/26/17.
 */

public class NetworkException extends Exception {

    private TYPE type;
    private String message;

    public enum TYPE{
        NO_CONNECTION,
        JSON,
        UNKNOWN
    }

    public NetworkException(TYPE type, String message){
        this.type = type;
        this.message = message;
    }

    public String getMessage(){
        return message;
    }

    public TYPE getType(){
        return type;
    }
}
