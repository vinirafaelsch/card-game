package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

import util.Estados;
import util.Mensagem;
import util.Status;

import javax.crypto.spec.OAEPParameterSpec;

/**
 * @author vinirafaelsch
 */
class Jogador implements Runnable {

    Socket socket;
    Server server;
    ObjectOutputStream output;
    ObjectInputStream input;

    int id;
    private String nome;

    private Jogador invite;
    private String ultimaJogada;
    private Estados estado;

    private Jogo jogo;

    List<Card> deck;

    public Jogador(Socket socket, Server server, int id) {
        this.id = id;
        this.socket = socket;
        this.server = server;
        estado = Estados.CONECTADO;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void run() {
        //trataConexão
        trataConexão(socket);
    }

    protected void enviaMsgAoCliente(Mensagem m) throws IOException {

        m.setStatus(Status.OK);
        output.writeUTF(m.toString());
        output.flush();
    }

    private void trataConexão(Socket socket) {
        //tratamento da comunicação com um cliente (socket)

        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
            output.flush();
            System.out.println("Conexao recebida, inciando protocolo...");
            //iniciar a conversa --- SINCRONO
            String msgResposta = "";
            String operacao = "";
            //Armazena o estado da comunicação com o cliente

            boolean primeira = true;
            //event loop
            do {
                //leitura
                String msgCliente = input.readUTF(); //bloqueante
                String response = "";
                System.out.println("Mensagem recebida do cliente: " + msgCliente);
                //escrita

                String[] protocolo = msgCliente.split(";");
                operacao = protocolo[0];
                Mensagem resposta = new Mensagem(operacao.toUpperCase() + "RESPONSE");
                switch (estado) {
                    case CONECTADO: {
                        switch (operacao) {
                            case "LOGIN": {
                                try {
                                    if (server.autenticados < 10) {
                                        Mensagem m = Mensagem.parseString(msgCliente);
                                        String name = m.getParam("nome");
                                        this.nome = name;
                                        estado = Estados.AUTENTICADO;
                                        server.autenticados++;
                                        resposta.setStatus(Status.OK);
                                    } else {
                                        resposta.setStatus(Status.ERROR);
                                        resposta.setParam("error", "Limite de usuários autenticados excedido");
                                    }
                                } catch (Exception e) {
                                    resposta.setStatus(Status.ERROR);
                                    resposta.setParam("error", "Mensagem Inválida ou não autorizada!");
                                }
                                break;
                            }
                            default: {
                                resposta.setStatus(Status.ERROR);
                                resposta.setParam("error", "Mensagem Inválida ou não autorizada!");
                                break;
                            }
                        }
                        break;
                    }
                    case AUTENTICADO: {
                        switch (operacao) {
                            //tratamento somente das mensagens possíveis no estado AUTENTICADO
                            case "CONVIDAR": {
                                /*
                                 CONVIDAR
                                 id:int
                                 */
                                try {
                                    Mensagem m = Mensagem.parseString(msgCliente);
                                    int idAdv = Integer.parseInt(m.getParam("id"));
                                    //notifica convite ao convidado
                                    Jogador guest = server.getClienteById(idAdv);
                                    /*
                                     CONVITE
                                     id:int //id de qm convidou
                                     */
                                    if (guest.getEstado() != Estados.ESPERANDO) {
                                        Mensagem convite = new Mensagem("CONVITE");
                                        convite.setParam("id", "" + id);

                                        guest.enviaMsgAoCliente(convite);

                                        guest.invite = this;

                                        resposta.setStatus(Status.OK);
                                        estado = Estados.ESPERANDO;
                                    } else {
                                        resposta.setStatus(Status.OK);
                                        resposta.setParam("error", "Jogador aguardando resposta");
                                    }
                                    //timeout
                                    //responder mensagem do "convidador"

                                } catch (Exception e) {
                                    resposta.setStatus(Status.ERROR);
                                    resposta.setParam("error", "Mensagem Inválida ou não autorizada!");
                                }
                                break;
                            }
                            case "CONVIDARTODOS": {
                                try {
                                    /*
                                     CONVITE
                                     null
                                     */
                                    Mensagem convite = new Mensagem("CONVITETODOS");
                                    server.broadcast(convite, this);

                                    resposta.setStatus(Status.OK);
                                    estado = Estados.ESPERANDO;
                                    //timeout
                                    //responder mensagem do "convidador"
                                } catch (Exception e) {
                                    resposta.setStatus(Status.ERROR);
                                    resposta.setParam("error", "Mensagem Inválida ou não autorizada!");
                                }
                                break;
                            }
                            case "CONVITE": {
                                try {
                                    Mensagem m = Mensagem.parseString(msgCliente);
                                    Mensagem reply = new Mensagem("RESPONDECONVITE");

                                    String res = m.getParam("response");

                                    reply.setParam("response", res);
                                    reply.setParam("id", "" + this.invite.id);
                                    reply.setParam("id2", "" + id);

                                    Jogador j = this.invite;
                                    j.enviaMsgAoCliente(reply);

                                    if (res.equals("ACCEPT") &&
                                            this.getEstado() != Estados.JOGANDO &&
                                            this.invite.getEstado() != Estados.JOGANDO) {
                                        server.criarJogo(this, this.invite);
                                    } else {
                                        resposta.setParam("error", "O convite foi recusado ou o Jogador está em outra partida");
                                    }

                                    reply.setStatus(Status.OK);
                                    resposta.setStatus(Status.OK);

                                } catch (Exception e) {
                                    resposta.setStatus(Status.ERROR);
                                    resposta.setParam("error", "Mensagem Inválida ou não autorizada!");
                                }
                                break;
                            }
                            case "CONVITETODOS": {
                                try {
                                    Mensagem m = Mensagem.parseString(msgCliente);
                                    Mensagem reply = new Mensagem("RESPONDECONVITETODOS");

                                    String res = m.getParam("response");

                                    reply.setParam("response", res);
                                    reply.setParam("id", "" + this.invite.id);
                                    reply.setParam("id2", "" + id);

                                    Jogador j = this.invite;
                                    j.enviaMsgAoCliente(reply);

                                    if (res.equals("ACCEPT") && this.getEstado() != Estados.JOGANDO
                                            && this.invite.getEstado() != Estados.JOGANDO) {
                                        server.criarJogo(this, this.invite);
                                        this.setEstado(Estados.JOGANDO);
                                        this.invite.setEstado(Estados.JOGANDO);
                                    } else {
                                        resposta.setParam("error", "O convite foi recusado ou o Jogador está em outra partida");
                                    }

                                    reply.setStatus(Status.OK);
                                    resposta.setStatus(Status.OK);

                                } catch (Exception e) {
                                    resposta.setStatus(Status.ERROR);
                                    resposta.setParam("error", "Mensagem Inválida ou não autorizada!");
                                }
                                break;
                            }
                            case "LOGOUT": {
                                estado = Estados.CONECTADO;
                                response = "LOGOUTRESPONSE";
                                response += "\n200";
                                break;
                            }
                            default: {
                                //mensagem inválida
                                resposta.setStatus(Status.ERROR);
                                resposta.setParam("error", "Mensagem Inválida ou não autorizada!");
                                break;
                            }
                        }
                        break;
                    }
                    case ESPERANDO: {
                        switch (operacao) {
                            case "ACCEPT": {
                                System.out.println("ACCEPTADA");
                                break;
                            }
                            case "REFUSE": {
                                resposta.setStatus(Status.OK);
                                estado = Estados.CONECTADO;
                                break;
                            }
                        }
                        break;
                    }
                    case JOGANDO: {
                        switch (operacao) {
                            case "JOGADA": {
                                /**
                                 * JOGADA;opcao:stamina
                                 * JOGADA;opcao:strength
                                 */

                                try {
                                    Mensagem m = Mensagem.parseString(msgCliente);
                                    String opcao = m.getParam("opcao"); // stamina or strength or defense

                                    String jogadaStatus = this.processaJogada(opcao);

                                    resposta.setStatus(Status.OK);
                                    resposta.setParam("STATUS", "" + jogadaStatus);
                                } catch (Exception e) {
                                    resposta.setStatus(Status.ERROR);
                                    resposta.setParam("error", "Mensagem Inválida ou não autorizada!");
                                }
                                break;
                            }
                        }
                        break;
                    }
                }
                //enviar a resposta ao cliente
                //output.writeUTF(response);
                output.writeUTF(resposta.toString());
                output.flush();
            } while (!operacao.equals("pare"));
        } catch (Exception e) {
            System.out.println("Erro no loop de tratamento do cliente: " + socket.getInetAddress().getHostAddress());
        } finally {
            try {
                //fechar as conexões
                output.close();
                input.close();
                socket.close();
            } catch (IOException ex) {
                System.out.println("Erro normal ao fechar conexão do cliente..." + ex.getMessage());
            }

        }

    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Estados getEstado() {
        return estado;
    }

    public void setEstado(Estados estado) {
        this.estado = estado;
    }

    public Jogador getInvite() {
        return invite;
    }

    public void setInvite(Jogador invite) {
        this.invite = invite;
    }

    public Jogo getJogo() {
        return jogo;
    }

    public void setJogo(Jogo jogo) {
        this.jogo = jogo;
    }

    private synchronized String processaJogada(String opcao) throws IOException {
        if (!this.estado.equals(Estados.JOGANDO)) {
            return "Somente jogadores que estão em uma partida podem executar uma jogada!";
        }

        this.ultimaJogada = opcao;

        if (this.getAdversario().ultimaJogada != null) {

            Integer escolha1 = this.processaEscolha();
            Integer escolha2 = this.getAdversario().processaEscolha();

            Jogador vencedor = this.processaVencedor(escolha1, escolha2);

            /**
             * reseta as jogadas
             */
            ultimaJogada = null;
            getAdversario().ultimaJogada = null;

            String retorno = "", status = "";
            Mensagem convite = new Mensagem("JOGADARESPONSE");

            if (vencedor == null) {
                status = "OUVE EMPATE";
                retorno = "OUVE EMPATE";
            } else if (vencedor.equals(this)) {
                /// troca as cartas
                Card carta = getAdversario().getCartaAtual();
                getAdversario().deck.remove(carta);
                this.deck.add(carta);

                status = "VOCE PERDEU";
                retorno = "VOCE VENCEU";
            } else {
                /// troca as cartas
                Card carta = this.getCartaAtual();
                this.deck.remove(carta);
                getAdversario().deck.add(carta);

                status = "VOCE VENCEU";
                retorno = "VOCE PERDEU";
            }

            if (this.deck.isEmpty() || getAdversario().deck.isEmpty()) {
                /// fim de jogo
                String winnerMsg = "";
                if (this.deck.isEmpty()) {
                    winnerMsg = this.nome + " venceu!";
                } else {
                    winnerMsg = this.getAdversario().nome + " venceu!";
                }

                this.server.jogos.remove(this.getJogo());
                this.getAdversario().estado = Estados.AUTENTICADO;
                this.estado = Estados.AUTENTICADO;

                status = "O JOGO ACABOU " + winnerMsg;
                retorno = "O JOGO ACABOU " + winnerMsg;
            } else {
                this.enviaMsgAoCliente(this.getCartaAtualInfo());
                this.getAdversario().enviaMsgAoCliente(this.getAdversario().getCartaAtualInfo());
            }

            convite.setParam("STATUS", status);
            getAdversario().enviaMsgAoCliente(convite);

            return retorno;
        } else {
            /// aguarda adversario jogar
            return "Agurde o outro jogador!";
        }
    }

    private Jogador getAdversario() {
        if (jogo == null) {
            return null;
        }
        return (jogo.player1 != this) ? jogo.player1 : jogo.player2;
    }

    private Card getCartaAtual() {
        return this.deck.get(0);
    }

    public Mensagem getCartaAtualInfo() {
        Card card = getCartaAtual();
        String in = card.getName() + ":stamina" + card.getStamina() + ";strength:" + card.getStrength() + ";defense:" + card.getDefense() + ";";

        Mensagem info = new Mensagem("CARTAATUALRESPONSE");
        info.setParam("CARTA", in);

        return info;
    }

    private Integer processaEscolha() {
        if (this.ultimaJogada.equalsIgnoreCase("stamina")) {
            return this.getCartaAtual().getStamina();
        } else if (this.ultimaJogada.equalsIgnoreCase("strength")) {
            return this.getCartaAtual().getStrength();
        } else {
            //// defense
            return this.getCartaAtual().getDefense();
        }
    }

    private Jogador processaVencedor(Integer escolha1, Integer escolha2) {
        if (escolha1 > escolha2) {
            return this;
        } else if (escolha1 < escolha2) {
            return this.getAdversario();
        } else {
            return null;
        }
    }
}
