//package main;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

class Main {

    static TreeMap<Integer, List<Vertice>> listaVertices = new TreeMap<>();
    static ArrayList<Vertice> arrayListaVertices = new ArrayList<>();
    static TreeMap<Double, Solucao> listaSolucoes = new TreeMap<>();
    static int run_codes = 0;
    static int quantidade_mutacao = 0;
    static int qdePecas = 0;
    static int qdeMedianas = 0;

    static int qdePopulacao = 1000;
    static int taxaMutacao = 3;
    static int bitsMutacao = 2;
    static int qdeSorteio = 30;
    static int pontoParada = 1000;
    static int tipoCruzamento = 0; //0->aleatorio, 1->intersessao*/
    static int tipoMutacao = 0; // 0->aleatorio, 1->bits proximos*/

    static void debug() {
        System.out.println(".::Debug::.");
    }

    static void exit() {
        Main.exit("exit");
    }

    static void exit(String s) {
        System.out.println(s);
        System.exit(0);
    }

    static public void main(String[] args) throws IOException {
        long time_init = System.currentTimeMillis();
        Leitura leitura = new Leitura();
        Relatorio relatorio = new Relatorio();
        Solucao s = new Solucao(leitura.readFile("caso1.txt", Main.run_codes));

        if (Main.run_codes == 0) {
            System.out.println("Tempo de leitura da entrada: " + ((System.currentTimeMillis() - time_init) / 1000) + "s ");
            time_init = System.currentTimeMillis();
        }
//        Genetico.calculaDistanciasVertices();
        if (Main.run_codes == 0) {
            System.out.println("Calculando distancia vertices: " + ((System.currentTimeMillis() - time_init) / 1000) + "s ");
            time_init = System.currentTimeMillis();
        }
        qdePecas = leitura.qdePecas;
        qdeMedianas = leitura.qdeMedianas;
        int iteracoes = 0;
        int countParada = 0;

        int size_solucoes = listaSolucoes.size();
        while (size_solucoes < qdePopulacao) {
            Solucao solucao = new Solucao();
            solucao.iniciaPopulacaoAleatoria(qdeMedianas, qdePecas);
            solucao.calculaCusto();
//            solucao.verificaMedianasRepetidas();
            listaSolucoes.put(solucao.custo, solucao);
            size_solucoes++;
        }
        if (Main.run_codes == 0) {
            System.out.println("Tempo para gerar populacao inicial aleatoria: " + ((System.currentTimeMillis() - time_init) / 1000) + "s ");
            time_init = System.currentTimeMillis();
        }

        Solucao solucao1;
        Solucao solucao2;
        Solucao nova_solucao;

        while (countParada <= pontoParada && listaSolucoes.firstEntry().getValue().custo > 0) {
            solucao1 = Genetico.torneio(listaSolucoes, qdeSorteio);
            solucao2 = Genetico.torneio(listaSolucoes, qdeSorteio);
            if (solucao1.custo == solucao2.custo) {
                continue;
            }
            nova_solucao = Genetico.cruzar(solucao1, solucao2, tipoCruzamento);
//            nova_solucao.verificaMedianasRepetidas();
            nova_solucao = Genetico.mutacao(nova_solucao, taxaMutacao, bitsMutacao);
//            nova_solucao.verificaMedianasRepetidas();
            if (nova_solucao.custo < listaSolucoes.lastEntry().getKey() && !listaSolucoes.containsKey(nova_solucao.custo)) {
                if (nova_solucao.custo < listaSolucoes.firstEntry().getKey() && Main.run_codes == 0) {
                    System.out.println(iteracoes + " Tamanho-> " + listaSolucoes.size() + " - Melhor-> " + listaSolucoes.firstEntry().getKey() + " Pior-> " + listaSolucoes.lastEntry().getKey());
                }
                countParada = 0;
                listaSolucoes.remove(listaSolucoes.lastEntry().getKey());
                listaSolucoes.put(nova_solucao.custo, nova_solucao);
                if (Main.run_codes == 0) {
                    relatorio.add(iteracoes, listaSolucoes.firstEntry().getKey());
                }
            }
            countParada++;
            iteracoes++;
        }
        if (Main.run_codes == 0) {
            System.out.println("Tempo para encontrar melhor solucao local: " + ((System.currentTimeMillis() - time_init) / 1000) + "s ");
            System.out.println("Memoria usada->" + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (double) (1024 * 1024)));
            System.out.println(".::Melhor solucao::. " + listaSolucoes.firstEntry().getKey());
            relatorio.geraRelatorio();
        }
//        System.out.println(listaSolucoes.firstEntry().getValue().medianas);
        if (Main.run_codes == 1) {
            System.out.println(listaSolucoes.lastEntry().getKey());
        }
//        System.out.println(listaSolucoes.firstEntry().getKey());
    }

    private static class Relatorio {

        TreeMap<Integer, Double> iteracoes = new TreeMap<>();

        void add(Integer iter, Double melhor) {
            iteracoes.put(iter, melhor);
        }

        void geraRelatorio() throws IOException {
            System.out.println("Iniciando escrita relatorio");
            String strprint = "var geneticData = [";
            for (Map.Entry<Integer, Double> entry : iteracoes.entrySet()) {
                strprint += "[" + entry.getKey() + "," + entry.getValue() + "],";
            }
            strprint += "];";
            escreveRelatorioJs(strprint);
            System.out.println("Fim escrita relatorio");
        }

        void escreveRelatorioJs(String data) throws IOException {
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream("../../grafico/data.js"), "utf-8"))) {
                writer.write(data);
            }
        }
    }

    private static class Genetico {

        static Solucao buscaLocal(Solucao solucao) {
            Genetico.getMelhorVizinhoTipo1(solucao);
            return solucao;
        }

        /**
         * Dada uma solucao S, gerar N vizinhos onde e trocada uma mediana 1 vez
         * para cada vizinho, de maneira que esta mediana seja removida e subs
         * tituida por um de seus vértices ligados. Este vertice e substituido
         * aleatoriamente se tipo=0, Este vertice e substituido aleatoriamente
         * dentre os N mais próximos se tipo=1,
         */
        static Solucao getMelhorVizinho(Solucao solucao, int tipo, int N) {
            List<Mediana> listaM = new ArrayList<>();
            int size = solucao.medianas.size();
            List<Solucao> vizinhos = new ArrayList<>();
            Solucao melhor_vizinho = null;
            Solucao vizinho;
            int j;
            for (int i = 0; i < size; i++) {
                vizinho = new Solucao();
                j = 0;
                for (Mediana m : solucao.medianas) {
                    if (i == j) {
                        if (tipo == 0) {
                            vizinho.medianas.add(Genetico.getMelhorVizinhoTipo1(solucao, m));
                        } else {
                            vizinho.medianas.add(Genetico.getMelhorVizinhoTipo2(solucao, m, N));
                        }
                    } else {
                        vizinho.medianas.add(new Mediana(m));
                    }
                    j++;
                }
                vizinho.calculaCusto();
                if (melhor_vizinho == null || melhor_vizinho.custo > vizinho.custo) {
                    melhor_vizinho = vizinho;
                }
            }
            return melhor_vizinho;
        }

        static Mediana getMelhorVizinhoTipo1(Solucao solucao, Mediana m) {
            int random;
            Mediana retorno;
            random = (int) (Math.random() * m.lista_vertices.size());
            Vertice v = m.lista_vertices.get(random);
            while (solucao.containsV(v)) {
                random = (int) (Math.random() * m.lista_vertices.size());
                v = m.lista_vertices.get(random);
            }
            retorno = new Mediana(v.id);
            retorno.vertice_mediana = v;
            return retorno;
        }

        static Mediana getMelhorVizinhoTipo2(Solucao solucao, Mediana m, int taxaQde) {
            int random;
            TreeMap<Double, Vertice> listaDistancias = new TreeMap<>();
            for(Vertice v : m.lista_vertices){
            listaDistancias.put(Double.NaN, v);
            estou aqui, colocar dentro o treemap, ordenar e retornar
                    fazer best improvement na funcao pai, de maneira que retorna assim
                            que encontrar um sucessor melhor.;
pensar se inverter a ordem do tipo 1 ou 2, tendo em vista que 2 é melhor, porem mais custoso
        talvez manter a ordem
            }
            Mediana retorno;
            random = (int) (Math.random() * m.lista_vertices.size());
            Vertice v = m.lista_vertices.get(random);
            while (solucao.containsV(v)) {
                random = (int) (Math.random() * m.lista_vertices.size());
                v = m.lista_vertices.get(random);
            }
            retorno = new Mediana(v.id);
            retorno.vertice_mediana = v;
            return retorno;
        }
//        static List<Mediana> geraVizinhoTipo2(Solucao solucao){
//        }

        static Solucao cruzar(Solucao solucao1, Solucao solucao2, int tipoCruzamento) {
            ArrayList<Mediana> medianas_cruzadas = null;
            Solucao retorno = null;
            switch (tipoCruzamento) {
                case 1:
                    retorno = cruzaMedianasIntersessao(solucao1, solucao2);
                    break;
                case 0:
                default:
                    retorno = cruzaMedianasBitsAleatorios(solucao1, solucao2);
                    break;
            }
            return retorno;
        }

        static Solucao cruzaMedianasBitsAleatorios(Solucao solucao1, Solucao solucao2) {
            int tamanho_medianas_solucao = solucao1.medianas.size();
            Solucao sol_temp = new Solucao();
//            int i = (int) Math.floor(Math.random() * 2);
            Solucao retorno1 = new Solucao();
            Mediana m1;
            Mediana m2;
            for (int i = 0; i <= solucao1.medianas.size() - 1; i++) {
                m1 = solucao1.medianas.get(i);
                m2 = solucao2.medianas.get(i);
                if (!sol_temp.containsV(m1.vertice_mediana)) {
                    sol_temp.medianas.add(new Mediana(m1));
                }
                if (!sol_temp.containsV(m2.vertice_mediana)) {
                    sol_temp.medianas.add(new Mediana(m2));
                }
            }
            Collections.shuffle(sol_temp.medianas);
            Solucao retorno2 = new Solucao();
            int j = sol_temp.medianas.size() - 1;
            for (int i = 0; i <= solucao1.medianas.size() - 1; i++) {
                retorno1.medianas.add(sol_temp.medianas.get(i));
                retorno2.medianas.add(sol_temp.medianas.get(j));
                j--;
            }

            retorno1.calculaCusto();
            retorno2.calculaCusto();

//            System.out.println("s1 -> " + solucao1.custo);
//            System.out.println("s2 -> " + solucao2.custo);
//            System.out.println("r1 -> " + retorno1.custo);
//            System.out.println("r2 -> " + retorno2.custo);
            if (retorno1.custo < retorno2.custo) {
                return retorno1;
            }
            return retorno2;
        }

        static ArrayList<Mediana> cruzaMedianasBitsAleatorios_old(Solucao solucao1, Solucao solucao2) {
            int tamanho_medianas_solucao = solucao1.medianas.size();
            int random;
            ArrayList<ArrayList<Mediana>> intersessaoDisjuncao = solucao1.intersessaoDesjuncao(solucao2);
            ArrayList<Mediana> novas_medianas = new ArrayList<>();
            ArrayList<Mediana> merge_medianas = new ArrayList<>(solucao1.medianas);
            merge_medianas.removeAll(solucao2.medianas);
            merge_medianas.addAll(solucao2.medianas);
            Mediana medianaAdd;
            int tamanho_novas = 0;
            while (tamanho_novas < tamanho_medianas_solucao) {
                random = (int) (Math.random() * merge_medianas.size());
                medianaAdd = new Mediana(merge_medianas.get(random));
                novas_medianas.add(medianaAdd);
                merge_medianas.remove(random);
                tamanho_novas++;
            }
            return novas_medianas;
        }

        static void calculaDistanciasVertices() {
            Vertice v1;
            Double distancia;
            List<Vertice> vertices1 = new ArrayList<>();
            for (Vertice v : arrayListaVertices) {
                vertices1.add(v);
            }
            for (Vertice v : arrayListaVertices) {
                for (int i = 0; i <= vertices1.size() - 1; i++) {
                    v1 = vertices1.get(i);
                    distancia = v.calculaDistanciaVertices(v1);
//                    if (distancia != 0) {
                    v.distanciaVertices = CustomTreeMap.addTreemap(v.distanciaVertices, distancia, v1);
                    v.distanciaVerticesHash.put(v1.hashCode(), distancia);
//                    }
                }
            }
//            for (Map.Entry<Integer, List<Vertice>> entry : listaVertices.entrySet()) {
//                for (Vertice v : entry.getValue()) {
//                    System.out.println(v.distanciaVerticesHash.size());
//                }
//            }

        }

        static Solucao cruzaMedianasIntersessao(Solucao solucao1, Solucao solucao2) {
            int tamanho_medianas_solucao = solucao1.medianas.size();
            int random;
            ArrayList<ArrayList<Mediana>> intersessaoDisjuncao = solucao1.intersessaoDesjuncao(solucao2);
            ArrayList<Mediana> novas_medianas = intersessaoDisjuncao.get(0);
            ArrayList<Mediana> desjuncao_medianas = intersessaoDisjuncao.get(1);

            int tamanho_novas = novas_medianas.size();
            while (tamanho_novas < tamanho_medianas_solucao) {
                random = (int) (Math.random() * desjuncao_medianas.size());
                novas_medianas.add(desjuncao_medianas.get(random));
                desjuncao_medianas.remove(random);
                tamanho_novas++;
            }
            Solucao s = new Solucao();
            s.medianas = novas_medianas;
//            s.calculaCusto();
            return s;
        }

        static Solucao mutacao_aleatoria(Solucao solucao, int taxa_mucacao, int qde_bits) {
            int random;
            random = (int) Math.floor(Math.random() * 101);
            if (random < taxa_mucacao) {
                Main.quantidade_mutacao++;
                List<Mediana> novas_medianas = new ArrayList<>();
                Mediana novaMediana;
                Vertice randomVertice;
                Integer randomIndex;
                while (qde_bits > 0) {
                    randomVertice = Solucao.arrayListaVertices.get((int) Math.floor(Math.random() * (Solucao.arrayListaVertices.size())));
                    if (!solucao.containsV(randomVertice)) {
                        random = (int) (Math.random() * solucao.medianas.size());//index da mediana que sera substituida
                        solucao.medianas.remove(random);
                        novaMediana = new Mediana(randomVertice.id);
                        novaMediana.vertice_mediana = randomVertice;
                        solucao.medianas.add(novaMediana);
                        qde_bits--;
                    }
                }
//            System.out.println(solucao.medianas);
            }
            solucao.calculaCusto();
            return solucao;
        }

        static Solucao mutacao_proxima(Solucao solucao, int taxa_mucacao, int qde_bits) {
            /*NAO USADO POR CONSELHO DO PROFESSOR*/
            if (true) {
                return new Solucao();
            }
            int random;
            random = (int) Math.floor(Math.random() * 101);
            if (random < taxa_mucacao) {
                Main.quantidade_mutacao++;
                List<Mediana> novas_medianas = new ArrayList<>();
                Mediana novaMediana;
                Vertice randomVertice;
                Integer maxRand;
//                System.out.println(" init mut " + solucao.medianas);
                while (qde_bits > 0) {
                    random = (int) (Math.random() * solucao.medianas.size());//index da mediana que sera substituida
                    Mediana m = solucao.medianas.get(random);
                    List<Double> keys = new ArrayList<>(m.vertice_mediana.distanciaVertices.keySet());
                    maxRand = (int) Math.floor((keys.size() / 30));
                    Random randomgg = new Random();
                    Double randomKey = keys.get(randomgg.nextInt(maxRand));
                    List<Vertice> l = m.vertice_mediana.distanciaVertices.get(randomKey);
                    randomVertice = l.get((int) Math.floor(Math.random() * (l.size())));

                    if (!solucao.containsV(randomVertice)) {
                        random = (int) (Math.random() * solucao.medianas.size());//index da mediana que sera substituida
                        solucao.medianas.remove(random);
                        novaMediana = new Mediana(randomVertice.id);
                        novaMediana.vertice_mediana = randomVertice;
                        solucao.medianas.add(novaMediana);
                        qde_bits--;
                    }
                }
//                System.out.println(solucao.medianas);
            }
            solucao.calculaCusto();
            return solucao;
        }

        static Solucao mutacao(Solucao solucao, int taxa_mucacao, int qde_bits) {
            if (Main.tipoMutacao == 0) {
                return mutacao_aleatoria(solucao, taxa_mucacao, qde_bits);
            } else {
                return mutacao_proxima(solucao, taxa_mucacao, qde_bits);

            }

        }

        /*verificar se para cruzar dois elementos, realizar 2x o algoritmo ou, 1x e utilizar os dois melhores*/
        static Solucao torneio(TreeMap<Double, Solucao> listaSolucoes, int num_elementos) {
            TreeMap<Double, Solucao> listaCompetidores = new TreeMap<>();
            List<Double> keys = new ArrayList<>(listaSolucoes.keySet());
            Random random = new Random();
            /*substituir por for (0,k) para ver qual e mais rapido*/
            while (listaCompetidores.size() < num_elementos) {
                Double randomKey = keys.get(random.nextInt(keys.size()));
                Solucao escolhido = listaSolucoes.get(randomKey);
                listaCompetidores.put(escolhido.custo, escolhido);
            }
            return listaCompetidores.firstEntry().getValue();
        }
    }

    private static class Solucao {

        static final AtomicInteger contador = new AtomicInteger(0);
        int id;
        static TreeMap<Integer, List<Vertice>> listaVertices = new TreeMap<>();
        static ArrayList<Vertice> arrayListaVertices = new ArrayList<>();

        List<Mediana> medianas = new ArrayList<>();
        double custo;

        public Solucao() {
            id = contador.incrementAndGet();
        }

        public Solucao(TreeMap<Integer, List<Vertice>> listaVertices) {
            Solucao.listaVertices = listaVertices;
            arrayListaVertices = Main.arrayListaVertices;

        }

        void iniciaPopulacaoAleatoria3(int qdeMedianas, int qdePecas) {
            int random;
            int maxRandom = (int) Math.floor(qdePecas / qdeMedianas);
            int size_mediana = medianas.size();
            while (size_mediana < qdeMedianas) {
                for (Map.Entry<Integer, List<Vertice>> entry : listaVertices.entrySet()) {
                    for (Vertice v : entry.getValue()) {
                        if (size_mediana < qdeMedianas) {
                            random = (int) (Math.random() * maxRandom);
                            if (random == 1) {
                                Mediana m = new Mediana(v.id);
                                m.vertice_mediana = v;
                                medianas.add(m);
                                size_mediana++;
                            }
                        } else {
                            break;
                        }
                    }
                }
            }
        }

        void iniciaPopulacaoAleatoria(int qdeMedianas, int qdePecas) {
            int maxRandom = (int) Math.floor(qdePecas / qdeMedianas);
            int size_mediana = medianas.size();
            List<Integer> keys = new ArrayList<Integer>(listaVertices.keySet());
            while (size_mediana < qdeMedianas) {
                Random random = new Random();
                Integer randomKey = keys.get(random.nextInt(keys.size()));
                List<Vertice> vertices = listaVertices.get(randomKey);
                Random random2 = new Random();
                Vertice v = vertices.get(random2.nextInt(vertices.size()));
                if (!this.containsV(v)) {
                    Mediana m = new Mediana(v.id);
                    m.vertice_mediana = v;
                    medianas.add(m);
                    size_mediana++;
                }
            }
        }

        void calculaCusto() {
            this.custo = 0;
            int countVertice = 0;
            Mediana m;
            for (Mediana me : medianas) {
                me.demanda_atual = 0;
            }
            for (Map.Entry<Integer, List<Vertice>> entry : listaVertices.entrySet()) {
                for (Vertice v : entry.getValue()) {
                    m = v.getMedianaProximaLivre(medianas);
                    if (m != null) {
                        this.custo += v.calculaDistanciaVertices(m.vertice_mediana);
                    } else {
                        this.custo = Double.MAX_VALUE;
                        break;
                    }
                    countVertice++;
                }
            }
//            System.out.println(" custo " + this.custo);
//            System.out.println(" numero de vertices2 " + countVertice);
        }

        ArrayList<ArrayList<Mediana>> intersessaoDesjuncao(Solucao other) {
            ArrayList<ArrayList<Mediana>> retorno = new ArrayList<>();
            ArrayList<Mediana> intersessao = new ArrayList<>();
            ArrayList<Mediana> desjuncao = new ArrayList<>();
            ArrayList<Mediana> thisM = new ArrayList<>(this.medianas.size());
            ArrayList<Mediana> otherM = new ArrayList<>(other.medianas.size());

            for (Mediana m1 : this.medianas) {
                thisM.add(new Mediana(m1));
            }
            for (Mediana m2 : other.medianas) {
                otherM.add(new Mediana(m2));
            }
//        for (Mediana m1 : thisM) {
//            for (Mediana m2 : otherM) {
//                if (m1.vertice_mediana.id == m2.vertice_mediana.id) {
//                    System.out.println("Entrou");
//                    intersessao.add(m1);
//                }
//            }
//        }
            for (int i = 0; i >= thisM.size() - 1; i++) {
                for (int j = 0; j >= otherM.size() - 1; j++) {
//                System.out.println(thisM.get(i).vertice_mediana.id + " - " + otherM.get(j).vertice_mediana.id);
                    if (thisM.get(i).vertice_mediana.id == otherM.get(j).vertice_mediana.id) {
//                    System.out.println("Entrou!");
                        intersessao.add(thisM.get(i));
                        thisM.remove(thisM.get(i));
                        otherM.remove(otherM.get(j));
                    }
                }
            }

            for (Mediana m1 : thisM) {
//            System.out.println("addedendo1-> " + m1.vertice_mediana.id);
//            System.out.println("size 1 -> " + desjuncao.size());
                desjuncao.add(m1);
            }
            for (Mediana m2 : otherM) {
//            System.out.println("addedendo2-> " + m2.vertice_mediana.id);
//            System.out.println("size 2 -> " + desjuncao.size());
                desjuncao.add(m2);
            }
            retorno.add(intersessao);
            retorno.add(desjuncao);
            return retorno;
        }

        boolean containsV(Vertice v) {
//        System.out.println("ContainsV" + v);
            if (v == null) {
                return true;
            }
            for (Mediana m : this.medianas) {
                if (m.id == v.id) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            String ls = System.getProperty("line.separator");
            String retorno;
            String strCusto = String.valueOf((int) custo);
            retorno = "Solucao-> ID[" + id + "], Custo[" + strCusto + "] " + ls;
//        for (Mediana m : medianas) {
//            retorno += m.toString();
//        }
            return retorno;
        }

        void verificaMedianasRepetidas() {
            int countMediana;
            for (Mediana m : this.medianas) {
                countMediana = 0;
                for (Mediana m2 : this.medianas) {
                    if (m.vertice_mediana.id == m2.vertice_mediana.id) {
                        countMediana++;
                        if (countMediana > 1) {
                            System.out.println("dumb -> " + m.vertice_mediana.id + " " + m2.vertice_mediana.id);
                            System.out.println(this.medianas);
                            Main.exit();

                            return;
                        }
                    }
                }
            }
        }
    }

    private static class Mediana {

        static final AtomicInteger contador = new AtomicInteger(0);
        int id;
        int demanda_atual;
        int qde_vertices = 0;
        Vertice vertice_mediana;
        List<Vertice> lista_vertices = new ArrayList<>();

        public Mediana(int i) {
//            id = contador.incrementAndGet();
            this.id = i;
        }

        /*para CLONE*/
        public Mediana(Mediana m) {
            this.id = m.id;
            this.vertice_mediana = m.vertice_mediana;
            this.demanda_atual = 0;
        }

        @Override
        public String toString() {
            String ls = System.getProperty("line.separator");
            String retorno;
//        retorno = "[" + id + "][" + vertice_mediana.id + "] Mediana: CAP[" + vertice_mediana.capacidade + "], DEM[" + demanda_atual + "]" + ls + vertice_mediana.toString() + ls;
            retorno = "Mediana -> ID_MED [" + vertice_mediana.id + "], ID_VERTICE [" + id + "], CAPACIDADE[" + vertice_mediana.capacidade + "], DEMANDA [" + demanda_atual + "], QDE VERT " + qde_vertices + ls;
//        PRINT MEDIANA!!!
//        for (Vertice v : lista_vertices) {
//            retorno += v.toString();
//        }
            return retorno;
        }
    }

    private static class Vertice {

        static final AtomicInteger contador = new AtomicInteger(0);
        TreeMap<Double, List<Vertice>> distanciaVertices = new TreeMap<>();
        HashMap<Integer, Double> distanciaVerticesHash = new HashMap<>();

        int id;
        int posX;
        int posY;
        int capacidade;
        int demanda;

        public Vertice() {
            id = contador.incrementAndGet();
        }

        public Vertice(int posX, int posY, int capacidade, int demanda) {
            this.posX = posX;
            this.posY = posY;
            this.capacidade = capacidade;
            this.demanda = demanda;
        }

        Mediana getMedianaProximaLivre(List<Mediana> medianas) {
            int capacidade_mediana, soma_cap_demanda;
            TreeMap<Double, List<Mediana>> listaDistancias = new TreeMap<>();
            int size = 0;
            Double distancia;
//          for (Mediana mediana : medianas) {
//              System.out.println(mediana.demanda_atual);
//          }
            for (Mediana mediana : medianas) {
                size++;
                distancia = this.calculaDistanciaVertices(mediana.vertice_mediana);
//                 distancia = this.distanciaVerticesHash.get(mediana.vertice_mediana.hashCode());

                listaDistancias = CustomTreeMap.addTreemap(listaDistancias, distancia, mediana);
            }

            for (Map.Entry<Double, List<Mediana>> entry : listaDistancias.entrySet()) {
                for (Mediana mediana : entry.getValue()) {
                    capacidade_mediana = mediana.vertice_mediana.capacidade;
                    soma_cap_demanda = (mediana.demanda_atual + this.demanda);
                    if (capacidade_mediana >= soma_cap_demanda) {
                        mediana.demanda_atual = soma_cap_demanda;
                        mediana.lista_vertices.add(this);
                        mediana.qde_vertices++;
                        return mediana;
                    }
                }
            }
//            System.out.println("Sem medianas com capacidades");
//            System.out.println("size treemap " + listaDistancias.size());
//            System.out.println("size " + listaDistancias.size());
//            for (Map.Entry<Double, List<Mediana>> entry : listaDistancias.entrySet()) {
//                for (Mediana mediana : entry.getValue()) {
//                    System.out.println(mediana);
//                }
//            }
//            Main.exit(" Erro! Nao foi encontrada mediana com espaco suficiente para ligar ao vertice");
//            System.out.println("Nao foi encontrada mediana com espaco suficiente para ligar ao vertice");
//            return new Mediana(0);
            return null;
        }

        Double calculaDistanciaVertices(Vertice vertice) {
            return this.calculaPitagoras(this.posX, vertice.posX, this.posY, vertice.posY);
        }

        Double calculaPitagoras(int x1, int x2, int y1, int y2) {
            Double retorno = Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
            return retorno;
//        return  Math.round(retorno * 100);
        }

        @Override
        public String toString() {
            String ls = System.getProperty("line.separator");
            String retorno = "Vertice-> ID [" + id + "], Demanda [" + demanda + "]" + ls;
            return retorno;
        }
    }

    private static class CustomTreeMap {

//    static Vertice getRandomVertice(TreeMap<Integer, List<Vertice>> treeMap, Integer randomIndex) {
//        List<Vertice> randomVertice;
//        System.out.println("Tree size= " + treeMap.size());
//        System.out.println("Index Param= " + randomIndex);
//        randomVertice = treeMap.get(randomIndex);
//        System.out.println("treemap with param = " + randomVertice);
//        int randint = (int) Math.floor(Math.random() * (randomVertice.size()));
//        System.out.println("Rand index = " + randint);
//        System.out.println("On size = " + randomVertice.size());
//        Vertice v = null;
//        v = randomVertice.get(randint);
//        System.out.println(v);
//        return v;
//    }
        static TreeMap<Integer, List<Vertice>> removeTreemap(TreeMap<Integer, List<Vertice>> treeMap, Integer valor, Vertice obj) {
            List<Vertice> tempList = treeMap.get(valor);
            tempList.remove(obj);
            treeMap.put(valor, tempList);
            return treeMap;
        }

        static TreeMap<Double, List<Vertice>> addTreemap(TreeMap<Double, List<Vertice>> treeMap, Double valor, Vertice obj) {
            List<Vertice> tempList = null;
            if (treeMap.containsKey(valor)) {
                tempList = treeMap.get(valor);
                if (tempList == null) {
                    tempList = new ArrayList<>();
                }
                tempList.add(obj);
            } else {
                tempList = new ArrayList<>();
                tempList.add(obj);
            }
            treeMap.put(valor, tempList);
            return treeMap;
        }

        static TreeMap<Integer, List<Vertice>> addTreemap(TreeMap<Integer, List<Vertice>> treeMap, Integer valor, Vertice obj) {
            List<Vertice> tempList = null;
            if (treeMap.containsKey(valor)) {
                tempList = treeMap.get(valor);
                if (tempList == null) {
                    tempList = new ArrayList<>();
                }
                tempList.add(obj);
            } else {
                tempList = new ArrayList<>();
                tempList.add(obj);
            }
            treeMap.put(valor, tempList);
            return treeMap;
        }

        static TreeMap<Double, List<Mediana>> addTreemap(TreeMap<Double, List<Mediana>> treeMap, Double valor, Mediana obj) {
            List<Mediana> tempList = null;
            if (treeMap.containsKey(valor)) {
                tempList = treeMap.get(valor);
                if (tempList == null) {
                    tempList = new ArrayList<>();
                }
                tempList.add(obj);
            } else {
                tempList = new ArrayList<>();
                tempList.add(obj);
            }
            treeMap.put(valor, tempList);
            return treeMap;
        }

//    static TreeMap<Double, List<Mediana>> addTreemap(TreeMap<Double, List<Mediana>> listaDistancias, Double calculaDistanciaVertices, Mediana mediana) {
//        return addTreemap(listaDistancias, calculaDistanciaVertices, mediana);
//    }
    }

    private static class Leitura {

        int qdePecas = 0;
        int qdeMedianas = 0;

        TreeMap<Integer, List<Vertice>> readFile(String arquivo, int runcodes) throws IOException {
            TreeMap<Integer, List<Vertice>> listaV = new TreeMap<>(Collections.reverseOrder());
            ArrayList<Vertice> arrayV = new ArrayList<>();
            int ind = 0;
            int soma_demanda = 0;
            Vertice v = new Vertice();
            Scanner scan;
            if (runcodes == 1) {
                scan = new Scanner(System.in);
            } else {
                scan = new Scanner(new FileReader(Main.class.getResource(arquivo).getPath()));
            }
            qdePecas = scan.nextInt();
            qdeMedianas = scan.nextInt();

            scan.nextLine();

            while (scan.hasNext()) {
                switch (ind % 4) {
                    case 0:
                        v = new Vertice();
                        v.posX = (int) Double.parseDouble(scan.next());
                        break;
                    case 1:
                        v.posY = (int) Double.parseDouble(scan.next());
                        break;
                    case 2:
                        v.capacidade = (int) Double.parseDouble(scan.next());
                        break;
                    case 3:
                        v.demanda = (int) Double.parseDouble(scan.next());
                        soma_demanda += v.demanda;
//                                listaV = CustomTreeMap.addTreemap(listaV, v.demanda, v);
                        listaV = CustomTreeMap.addTreemap(listaV, v.demanda, v);
                        arrayV.add(v);
                        break;
                    default:
                        break;
                }
                ind++;
            }
            scan.close();
            Main.listaVertices = listaV;
            Main.arrayListaVertices = arrayV;
            return listaV;
        }

        TreeMap<Integer, List<Vertice>> readFilegg(String arquivo) throws IOException {
            TreeMap<Integer, List<Vertice>> listaV = new TreeMap<>(Collections.reverseOrder());
            ArrayList<Vertice> arrayV = new ArrayList<>();
            String content = new String(Files.readAllBytes(Paths.get(arquivo)));
            String lines[] = content.split("[\\r\\n]+");
            int ind = 0;
            int soma_demanda = 0;
            Vertice v = new Vertice();
            for (String linha : lines) {
                String splits[] = linha.split(" ");
                for (String elem : splits) {
                    if (elem.length() > 0) {
                        if (ind == 0) {
                            qdePecas = Integer.parseInt(elem);
                        } else if (ind == 1) {
                            qdeMedianas = Integer.parseInt(elem);
                        } else {
                            switch ((ind - 2) % 4) {
                                case 0:
                                    v = new Vertice();
//                                v.posX = (int) Double.parseDouble(elem);
                                    v.posX = (int) Double.parseDouble(elem);
                                    break;
                                case 1:
                                    v.posY = (int) Double.parseDouble(elem);
                                    break;
                                case 2:
                                    v.capacidade = (int) Double.parseDouble(elem);
                                    break;
                                case 3:
                                    v.demanda = (int) Double.parseDouble(elem);
                                    soma_demanda += v.demanda;
//                                listaV = CustomTreeMap.addTreemap(listaV, v.demanda, v);
                                    listaV = CustomTreeMap.addTreemap(listaV, v.demanda, v);
                                    arrayV.add(v);
                                    break;
                                default:
                                    break;
                            }
                        }
                        ind++;
                    }
                }
            }

            Main.arrayListaVertices = arrayV;
//        System.out.println(arrayV);
            return listaV;

//                for (Map.Entry<Double, Mediana> entry : listaDistancias.entrySet()) {
//                }
//        System.out.println(m2.vertice_mediana);
//        Vertice v1 = new Vertice();
//        double dist = v1.calculaDistanciaVertices(m1.vertice_mediana, m2.vertice_mediana);
        }
    }
}
