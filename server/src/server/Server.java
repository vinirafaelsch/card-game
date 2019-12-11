package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import util.Estados;
import util.Mensagem;

/**
 * @author vinirafaelsch
 */
public class Server {

    private ServerSocket serverSocket;
    ArrayList<Thread> threads;
    ArrayList<Jogador> jogadores;
    ArrayList<Jogo> jogos;
    Map<String, Integer> rank;

    public Server() {
        threads = new ArrayList<>();
        jogadores = new ArrayList<>();
        jogos = new ArrayList<>();
        rank = new HashMap<>();
    }

    /**
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
        //manda pra todos, menos pro processo da tarefa
        for (Jogador t : jogadores) {
            if (t != emissor && t.getEstado() != Estados.ESPERANDO) {
                t.enviaMsgAoCliente(m);
                t.setInvite(emissor);
            }
        }
    }

    protected synchronized void criarJogo(Jogador emissor, Jogador guest) throws IOException {
        emissor.setEstado(Estados.JOGANDO);
        guest.setEstado(Estados.JOGANDO);

        Jogo novoJogo = new Jogo(emissor, guest);

        emissor.setJogo(novoJogo);
        guest.setJogo(novoJogo);

        this.jogos.add(novoJogo);

        emissor.enviaMsgAoCliente(emissor.getCartaAtualInfo());
        guest.enviaMsgAoCliente(guest.getCartaAtualInfo());

        System.out.println("Jogo entre " + emissor.getNome() + " e " + guest.getNome() + " foi iniciado!");
    }
    
    public void setParam(String chave, Integer valor) {
        rank.put(chave, valor);
    }

    public Integer getParam(String chave) {
        return rank.get(chave);
    }
}
