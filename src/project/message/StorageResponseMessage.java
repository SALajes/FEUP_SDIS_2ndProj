package project.message;

import java.math.BigInteger;

public class StorageResponseMessage extends BaseMessage{
    //TODO AQUI O IDEAL ERA O PEER QUE RECEBE RESPONDER EM CONFORMIDADE COM O TIPO PEDIDO
    // E ENVIAR, POR EXEMPLO, UM ARRAY DOS CHUNKS QUE CUMPREM O REQUISITO ( QUE TEM GUARDADO OU QUE QUER QUE
    // O EMISSOR GUARDE

    public StorageResponseMessage(BigInteger sender) {
        super(Message_Type.STORAGE_RESPONSE, sender);
    }
}
