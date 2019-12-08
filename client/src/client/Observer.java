package client;

import util.Mensagem;

/**
 *
 * @author vinirafaelsch
 */
public interface Observer<T extends Mensagem> {
    public void update( T content );
}
