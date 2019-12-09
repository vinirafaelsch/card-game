package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import util.Mensagem;

/**
 *
 * @author vinirafaelsch
 */
public class Server {

    private ServerSocket serverSocket;
    int porta;
    int autenticados = 0;
    ArrayList<Thread> threads;
    ArrayList<Jogador> jogadores;
    ArrayList<Jogo> jogos;

    public Server() {
        threads = new ArrayList<>();
        jogadores = new ArrayList<>();
        jogos = new ArrayList<>();
    }

    /**
     * @param args the command line arguments 1 - Criar o servidor de conexÃµes
     * 2 -Esperar o um pedido de conexÃ£o; // Outro processo 2.1 e criar uma
     * nova conexÃ£o; 3 - Criar streams de enechar socket de comunicaÃ§Ã£o entre
     * servidor/cliente 4.2 - Fechar streams de entrada e saÃ­da trada e saÃ­da;
     * 4 - Tratar a conversaÃ§Ã£o entre cliente e servidor (tratar protocolo);
     * 4.1 - Fechar socket de comunicaÃ§Ã£o entre servidor/cliente 4.2 - Fechar
     * streams de entrada e saÃ­da
     *
     *
     *
     *
     */
    protected void avisaServer(String msg) {
        System.out.println("Cliente avisou: " + msg);
    }

    private void init() throws IOException {

        try {
            serverSocket = criarServerSocket(5555);
            //2 -Esperar o um pedido de conexÃ£o;
            int id = 0;
            do {

                System.out.println("Esperando conexao...");
                Socket socket = esperaConexao(); //bloqueante
                //3 - Criar streams de enechar socket de comunicaÃ§Ã£o entre servidor/cliente
                //Criar outra thread para tratar cliente novo

                Jogador tarefa = new Jogador(socket, this, id++);
                jogadores.add(tarefa);
                Thread thread = new Thread(tarefa);
                threads.add(thread);
                thread.start();

                System.out.println("Conexão com cliente estabelecida.");

            } while (true);
        } catch (Exception e) {
            System.out.println("Erro no event loop do main(): " + e.getMessage());
            serverSocket.close();
        }
    }

    protected Jogador getClienteById(int id) {
        for (Jogador j : jogadores) {
            if (j.getId() == id) {
                return j;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        Server server = new Server();

        try {
            //1 - Criar o servidor de conexÃµes            
            server.init();

        } catch (Exception e) {
            e.printStackTrace();
            //System.out.println("Erro na main do ServerSocket " + e.);
            System.exit(0);
        }
    }

    private ServerSocket criarServerSocket(int porta) {
        try {
            this.serverSocket = new ServerSocket(porta);
        } catch (Exception e) {
            System.out.println("Erro na Criação do server Socket " + e.getMessage());
        }

        return serverSocket;
    }

    private Socket esperaConexao() {
        try {
            return serverSocket.accept();
        } catch (IOException ex) {
            System.out.println("Erro ao criar socket do cliente " + ex.getMessage());
            return null;
        }
    }

    /*Broadcast*/
    protected void broadcast(Mensagem m, Jogador emissor) throws IOException {
        if (emissor == null) {
            //manda para todos
            for (Jogador t : jogadores) {
                t.enviaMsgAoCliente(m);
            }
        } else {
            //manda pra todos, menos pro processo da tarefa
            for (Jogador t : jogadores) {
                if (t != emissor) {
                    t.enviaMsgAoCliente(m);
                }
            }
        }
    }

//    protected void criarJogo(Jogador emissor, Jogador guest) {
//
//    }
}