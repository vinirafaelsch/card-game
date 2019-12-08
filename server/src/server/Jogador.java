package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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

    public Jogador(Socket socket, Server server, int id) {
        this.socket = socket;
        this.server = server;
        this.id = id;
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
                            case "AVISATODOS":
                                /*
                                 AVISATODOS
                                 msg:String
                                
                                 AVISATODOSRESPONSE
                                 OK,ERROR
                                
                                 gera para todos
                                
                                 AVISO
                                 OK
                                 msg:String
                                 */

                                try {
                                    Mensagem broadcast = new Mensagem("AVISO");
                                    Mensagem msg = Mensagem.parseString(msgCliente);

                                    broadcast.setStatus(Status.OK);
                                    broadcast.setParam("msg", msg.getParam("msg"));

                                    resposta.setStatus(Status.OK);

                                    server.broadcast(msg, this);

                                } catch (Exception e) {
                                    resposta.setStatus(Status.ERROR);
                                    resposta.setParam("msg", "Erro ao enviar broadcast");
                                }

                                break;

                            case "SALDO":
                                resposta.setStatus(Status.OK);
                                resposta.setParam("saldo", "" + server.getSaldo());
                                break;
                            case "DEPOSITA":
                                //faz deposito na conta
                                /*
                                 DEPOSITA
                                 valor:int
                                 */
                                try {
                                    Mensagem msg = Mensagem.parseString(msgCliente);
                                    int valor = Integer.parseInt(msg.getParam("valor"));
                                    server.deposito(valor);
                                    resposta.setStatus(Status.OK);
                                } catch (Exception e) {
                                    resposta.setStatus(Status.ERROR);
                                    resposta.setParam("msg", "Faltou valor!");
                                }
                                break;
                            case "SAQUE":
                                /*
                                 SAQUE
                                 valor:int
                                 */
                                //garante o saque

                                try {
                                    Mensagem msg = Mensagem.parseString(msgCliente);
                                    int valor = Integer.parseInt(msg.getParam("valor"));
                                    server.saque(valor);
                                    resposta.setStatus(Status.OK);
                                } catch (Exception e) {
                                    resposta.setStatus(Status.ERROR);
                                    resposta.setParam("msg", "Faltou valor!");
                                }
                                break;
                            case "OI":
                                try {
                                    Mensagem mCliente = Mensagem.parseString(msgCliente);
                                    if (mCliente != null) {
                                        String nome = mCliente.getParam("nome");
                                        resposta = new Mensagem("OIRESPONSE");
                                        resposta.setStatus(Status.OK);
                                        resposta.setParam("mensagem", "Oi " + nome + ", Bem-vindo!");
                                    }
                                } catch (Exception e) {
                                }
                                break;
                            case "LOGIN":
                                try {

                                    estado = Estados.AUTENTICADO;
                                    resposta.setStatus(Status.OK);
                                } catch (Exception e) {
                                    resposta.setStatus(Status.ERROR);
                                    resposta.setParam("error", "Mensagem Inválida ou não autorizada!");
                                }
                                break;
                            default:
                                //mensagem inválida
                                resposta.setStatus(Status.ERROR);
                                resposta.setParam("error", "Mensagem Inválida ou não autorizada!");
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
                            break;
                            case "CHECK":
                                //validando protocolo (parse)
                                try {
                                    //tratamento da mensagem

                                } catch (Exception e) {

                                }
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

}
