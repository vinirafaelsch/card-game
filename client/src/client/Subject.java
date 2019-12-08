package client;

import util.Mensagem;

/**
 *
 * @author vinirafaelsch
 */
public interface Subject<T extends Mensagem> {
    public void subscribe( Observer<T> o );
    
}
