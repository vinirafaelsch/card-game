package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import util.Estados;
import util.Mensagem;
import util.Status;

/**
 *
 * @author vinirafaelsch
 */
class Jogador implements Runnable {

    Socket socket;
    Server server;
    ObjectOutputStream output;
    ObjectInputStream input;
    int id;
    Jogo jogo;
    private ArrayList<Jogador> invites;

    public Jogador(Socket socket, Server server, int id) {
        this.socket = socket;
        this.server = server;
        this.id = id;
        invites = new ArrayList<>();
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
            Estados estado = Estados.CONECTADO;

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
                                    Mensagem convite = new Mensagem("CONVITE");
                                    convite.setParam("id", "" + id);
                                    guest.enviaMsgAoCliente(convite);
                                    guest.getInvites().add(this);

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
                            case "CONVIDARTODOS": {
                                /*
                                 CONVIDAR
                                 id:int
                                 */
                                try {
                                    /*
                                     CONVITE
                                     null
                                     */
                                    Mensagem convite = new Mensagem("CONVITETODOS");
                                    convite.setParam("id", "" + id);
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

                            case "CONVITE":
                                Mensagem m = Mensagem.parseString(msgCliente);
                                Mensagem reply = new Mensagem("RESPONDECONVITE");

                                String res = m.getParam("response");
                                int id1 = Integer.parseInt(m.getParam("id"));

                                reply.setParam("response", res);
                                reply.setParam("id", "" + id1);
                                reply.setParam("id2", "" + id);

                                reply.setStatus(Status.OK);
                                resposta.setStatus(Status.OK);

                                Jogador j = server.getClienteById(id1);
                                j.enviaMsgAoCliente(reply);

                                break;

                            //exemplo com troca de estados
                            case "LOGOUT":
                                estado = Estados.CONECTADO;
                                response = "LOGOUTRESPONSE";
                                response += "\n200";
                                break;
                            default:
                                //mensagem inválida
                                resposta.setStatus(Status.ERROR);
                                resposta.setParam("error", "Mensagem Inválida ou não autorizada!");
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

    public ArrayList<Jogador> getInvites() {
        return invites;
    }

    public void setInvites(ArrayList<Jogador> invites) {
        this.invites = invites;
    }

    
    
}
