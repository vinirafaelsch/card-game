package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import util.Estados;
import util.Mensagem;
import util.Status;

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
                    case CONECTADO:
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
                            }
                            break;
                            default: {
                                resposta.setStatus(Status.ERROR);
                                resposta.setParam("error", "Mensagem Inválida ou não autorizada!");
                            }
                            break;
                        }
                        break;
                    case AUTENTICADO:

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
                            }
                            break;
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
                            }
                            break;
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
                            }
                            break;
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
                            }
                            break;
                            case "JOGADA": {
                                try {
                                    Mensagem m = Mensagem.parseString(msgCliente);
                                    ultimaJogada = m.getParam("opcao"); // stamina or strength or defense

                                    if (jogo.player2.ultimaJogada != null) {
                                        /// Quer dizer que o outro jogador ja efetuou sua jogada

                                        jogo.player1.ultimaJogada = null;
                                        jogo.player2.ultimaJogada = null;
                                    } else {
                                        /// Quer dizer que o outro jogador nao efetuou sua jogada
                                    }

                                    //notifica convite ao convidado
                                    Jogador guest = server.getClienteById(1);
                                    /*
                                     CONVITE
                                     id:int //id de qm convidou
                                     */
                                    Mensagem convite = new Mensagem("CONVITE");
                                    convite.setParam("id", "" + id);
                                    guest.enviaMsgAoCliente(convite);

                                    resposta.setStatus(Status.OK);
                                    estado = Estados.ESPERANDO;
                                    //timeout
                                    //responder mensagem do "convidador"

                                } catch (Exception e) {
                                    resposta.setStatus(Status.ERROR);
                                    resposta.setParam("error", "Mensagem Inválida ou não autorizada!");
                                }
                            }
                            case "LOGOUT": {
                                estado = Estados.CONECTADO;
                                response = "LOGOUTRESPONSE";
                                response += "\n200";
                            }
                            break;
                            default: {
                                //mensagem inválida
                                resposta.setStatus(Status.ERROR);
                                resposta.setParam("error", "Mensagem Inválida ou não autorizada!");
                            }
                            break;
                        }
                        break;

                    case ESPERANDO:
                        switch (operacao) {
                            case "ACCEPT":
                                System.out.println("ACCEPTADA");
                                break;
                            case "REFUSE":
                                resposta.setStatus(Status.OK);
                                estado = Estados.CONECTADO;
                                break;
                        }
                        break;
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
}
