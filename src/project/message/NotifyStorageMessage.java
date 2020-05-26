package project.message;

import java.math.BigInteger;
import java.util.ArrayList;

public class NotifyStorageMessage extends BaseMessage {
    private final ArrayList<Integer> chunk_numbers;
    private final String file_id;

    //TODO O IDEAL AQUI SERIA ELE RECEBER UMA FLAG A DIZER SE É UMA NOTIFICAÇÃO RELATIVA AOS SEUS BACK UP CHUNKS
    // OU AOS STORED CHUNKS, PARA QUE QUEM RECEBE POSSA PROCEDER EM CONFORMIDADE EM VEZ DE ESTARMOS A CRIAR DUAS
    // MESAGENS DIFERENTES PARA ISTO
    //private final String type;

    public NotifyStorageMessage(BigInteger sender, /* String type,*/ ArrayList<Integer> chunk_numbers, String file_id) {
        super(Message_Type.NOTIFY_STORAGE, sender);
        this.chunk_numbers = chunk_numbers;
        this.file_id = file_id;
        //this.type = type;
    }

    public String getFileId() {
        return file_id;
    }

    /*public String getType() {
        return type;
    }*/
}
