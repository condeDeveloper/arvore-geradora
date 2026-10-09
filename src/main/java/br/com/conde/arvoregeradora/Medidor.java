package br.com.conde.arvoregeradora;

/**
 * O MEDIDOR: imprime as tabelas que o README mostra e que os testes conferem.
 *
 * <p>Toda tabela daqui tem um teste do lado que afirma os mesmos numeros. Texto nao
 * quebra quando deixa de ser verdade; teste quebra.
 */
public final class Medidor {

    private Medidor() {
    }

    public static void main(String[] args) {
        String comando = args.length > 0 ? args[0] : "tudo";

        if (comando.equals("definicao") || comando.equals("tudo")) definicao();
        if (comando.equals("erro") || comando.equals("tudo")) erro();
        if (comando.equals("kirchhoff") || comando.equals("tudo")) kirchhoff();
        if (comando.equals("empates") || comando.equals("tudo")) empates();
        if (comando.equals("custo") || comando.equals("tudo")) custo();
    }

    private static void definicao() {
        System.out.println();
        System.out.println("OS TRES ALGORITMOS CONTRA A FORCA BRUTA, que testa todos os subconjuntos.");
        System.out.println();

        var linhas = Medidas.contraAForcaBruta();
        String[] nomes = linhas.get(0).pesos().keySet().toArray(new String[0]);

        StringBuilder cabecalho = new StringBuilder(String.format(
                "  %-26s %9s %9s %8s", "grafo", "vertices", "arestas", "conexo"));

        for (String n : nomes) cabecalho.append(String.format(" %12s", n));

        cabecalho.append(String.format(" %16s %8s", "pela forca bruta", "batem"));
        System.out.println(cabecalho);

        for (var l : linhas) {
            StringBuilder linha = new StringBuilder(String.format(
                    "  %-26s %9d %9d %8s", l.grafo(), l.vertices(), l.arestas(), l.conexo() ? "sim" : "NAO"));

            for (String n : nomes) {
                long p = l.pesos().get(n);

                linha.append(String.format(" %12s", p < 0 ? "nao ligou" : String.valueOf(p)));
            }

            linha.append(String.format(" %16s %8s", l.pelaForcaBruta(), l.todosBatem() ? "sim" : "NAO"));
            System.out.println(linha);
        }

        System.out.println();
    }

    private static void erro() {
        System.out.println();
        System.out.println("A VERSAO QUE USA UM CONJUNTO DE VISITADOS em vez de conjuntos disjuntos.");
        System.out.println();
        System.out.printf("  %-26s %9s %16s %12s %14s %7s %-40s%n",
                "grafo", "vertices", "arestas que deu", "peso certo", "peso da versao", "vale", "motivo");

        for (var l : Medidas.aVersaoPlausivel())
            System.out.printf("  %-26s %9d %16d %12d %14d %7s %-40s%n",
                    l.grafo(), l.vertices(), l.arestasDaVersao(), l.pesoCerto(), l.pesoDaVersao(),
                    l.vale() ? "sim" : "NAO", l.motivo());

        System.out.println();
        System.out.println("A CACA, em 200 grafos sorteados de cada densidade.");
        System.out.println();
        System.out.printf("  %-26s %8s %14s %10s %12s %14s%n",
                "familia", "grafos", "Kruskal errou", "Prim errou", "Boruvka errou", "visitados errou");

        for (var l : Medidas.cacarErros())
            System.out.printf("  %-26s %8d %14d %10d %12d %14d%n",
                    l.familia(), l.grafos(), l.kruskalErrou(), l.primErrou(),
                    l.boruvkaErrou(), l.visitadosErrou());

        System.out.println();
    }

    private static void kirchhoff() {
        System.out.println();
        System.out.println("O TEOREMA DE KIRCHHOFF, de 1847: o numero de arvores geradoras e um determinante.");
        System.out.println();
        System.out.printf("  %-26s %9s %9s %18s %18s %7s %24s%n",
                "grafo", "vertices", "arestas", "por Kirchhoff", "pela forca bruta", "batem",
                "mesmo apagando qualquer");

        for (var l : Medidas.oTeoremaDeKirchhoff())
            System.out.printf("  %-26s %9d %9d %18d %18s %7s %24s%n",
                    l.grafo(), l.vertices(), l.arestas(), l.porKirchhoff(), l.pelaForcaBruta(),
                    l.batem() ? "sim" : "NAO", l.mesmoApagandoQualquer() ? "sim" : "NAO");

        System.out.println();
        System.out.println("NO GRAFO COMPLETO: a formula de Cayley, de 1889, que e n elevado a n menos dois.");
        System.out.println();
        System.out.printf("  %9s %18s %24s %24s %18s %8s%n",
                "vertices", "em 64 bits", "em inteiro grande", "por Cayley", "coube em 64 bits", "batem");

        for (var l : Medidas.aFormulaDeCayley())
            System.out.printf("  %9d %18s %24s %24s %18s %8s%n",
                    l.vertices(), l.porKirchhoff(), l.emInteiroGrande(), l.pelaFormula(),
                    l.coubeEmSessentaEQuatro() ? "sim" : "NAO", l.batem() ? "sim" : "NAO");

        System.out.println();
    }

    private static void empates() {
        System.out.println();
        System.out.println("OS EMPATES DE PESO e as arvores diferentes com o mesmo peso.");
        System.out.println();
        System.out.printf("  %-26s %9s %18s %20s %24s %12s%n",
                "grafo", "arestas", "pesos distintos", "arvores minimas", "as tres dao a mesma", "peso comum");

        for (var l : Medidas.osEmpates())
            System.out.printf("  %-26s %9d %18d %20s %24s %12d%n",
                    l.grafo(), l.arestas(), l.pesosDistintos(),
                    l.arvoresMinimas() < 0 ? "nao cabe" : String.valueOf(l.arvoresMinimas()),
                    l.asTresDaoAMesmaArvore() ? "sim" : "nao", l.pesoComum());

        System.out.println();
        System.out.println("O QUE O DESEMPATE FAZ: o mesmo grafo com as arestas escritas ao contrario.");
        System.out.println();
        System.out.printf("  %-26s %18s %24s %24s %10s %16s%n",
                "grafo", "arvores minimas", "com desempate: a mesma", "sem desempate: a mesma",
                "peso", "peso ao contrario");

        for (var l : Medidas.oQueODesempateFaz())
            System.out.printf("  %-26s %18s %24s %24s %10d %16d%n",
                    l.grafo(),
                    l.arvoresMinimas() < 0 ? "nao cabe" : String.valueOf(l.arvoresMinimas()),
                    l.comDesempateDaAMesma() ? "sim" : "NAO",
                    l.semDesempateDaAMesma() ? "sim" : "NAO",
                    l.peso(), l.pesoAoContrario());

        System.out.println();
    }

    private static void custo() {
        System.out.println();
        System.out.println("O CUSTO de cada algoritmo, grafo por grafo.");
        System.out.println();

        var linhas = Medidas.oCusto();
        String[] nomes = linhas.get(0).passos().keySet().toArray(new String[0]);

        StringBuilder cabecalho = new StringBuilder(String.format(
                "  %-26s %9s %9s", "grafo", "vertices", "arestas"));

        for (String n : nomes) cabecalho.append(String.format(" %12s", n));

        System.out.println(cabecalho);

        for (var l : linhas) {
            StringBuilder linha = new StringBuilder(String.format(
                    "  %-26s %9d %9d", l.grafo(), l.vertices(), l.arestas()));

            for (String n : nomes) linha.append(String.format(" %12d", l.passos().get(n)));

            System.out.println(linha);
        }

        System.out.println();
        System.out.println("O CRESCIMENTO no grafo completo, onde as arestas crescem como n ao quadrado.");
        System.out.println();
        System.out.printf("  %9s %10s %14s %14s %14s%n", "vertices", "arestas", "Kruskal", "Prim", "Boruvka");

        for (var l : Medidas.oCrescimento())
            System.out.printf("  %9d %10d %14d %14d %14d%n",
                    l.vertices(), l.arestas(), l.kruskal(), l.prim(), l.boruvka());

        System.out.println();
    }
}
