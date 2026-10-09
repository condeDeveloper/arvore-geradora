package br.com.conde.arvoregeradora;

import java.util.ArrayList;
import java.util.List;

/**
 * O JUIZ, que sao TRES juizes independentes.
 *
 * <p><b>1. A definicao.</b> Um conjunto de arestas e arvore geradora quando tem n
 * menos uma arestas, liga todos os vertices e nao tem ciclo. Duas dessas tres
 * condicoes implicam a terceira, e o juiz confere as tres assim mesmo, porque
 * conferir duas e confiar num teorema que a implementacao poderia estar violando de
 * um jeito que as outras duas escondem.
 *
 * <p><b>2. A forca bruta.</b> Todos os subconjuntos de n menos uma arestas, filtrando
 * os que sao arvore geradora e pegando o de menor peso. Nao sabe o que e guloso, nao
 * sabe o que e corte, e nao tem como estar certa por acidente.
 *
 * <p><b>3. O TEOREMA DE KIRCHHOFF, de 1847.</b> O numero de arvores geradoras de um
 * grafo e o determinante de qualquer menor principal da matriz laplaciana. Ele nao
 * olha para aresta nenhuma uma por uma: ele monta uma matriz e tira um determinante.
 *
 * <p>O terceiro e o mais estranho e o mais forte. Contar arvores geradoras poderia
 * ser um problema de contagem dificil, como contar extensoes lineares ou calcular uma
 * permanente, e nao e: e um determinante, em tempo polinomial. A diferenca entre
 * esses casos e o assunto da tabela de Kirchhoff.
 */
public final class Juiz {

    private Juiz() {
    }

    /**
     * @param vale      o conjunto e uma arvore geradora
     * @param motivo    o que deu errado, se deu
     * @param peso      o peso total
     */
    public record Veredicto(boolean vale, String motivo, long peso) {
    }

    /** A DEFINICAO: n menos uma arestas, liga tudo, sem ciclo. */
    public static Veredicto conferir(Grafo g, List<Grafo.Aresta> arestas) {
        long peso = arestas.stream().mapToLong(Grafo.Aresta::peso).sum();

        if (g.vertices() == 0)
            return new Veredicto(arestas.isEmpty(), arestas.isEmpty() ? "" : "grafo vazio com arestas", peso);

        if (arestas.size() != g.vertices() - 1)
            return new Veredicto(false, "tem " + arestas.size() + " arestas e precisaria de "
                    + (g.vertices() - 1), peso);

        for (Grafo.Aresta a : arestas)
            if (!g.arestas().contains(a))
                return new Veredicto(false, "a aresta " + a + " nao esta no grafo", peso);

        Arvores.Conjuntos conjuntos = new Arvores.Conjuntos(g.vertices());

        for (Grafo.Aresta a : arestas)
            if (!conjuntos.juntar(a.u(), a.v()))
                return new Veredicto(false, "a aresta " + a + " fecha ciclo", peso);

        for (int v = 1; v < g.vertices(); v++)
            if (!conjuntos.juntos(0, v))
                return new Veredicto(false, "o vertice " + v + " ficou de fora", peso);

        return new Veredicto(true, "", peso);
    }

    // ───────────────────── 2. a forca bruta ─────────────────────

    /**
     * @param peso     o menor peso possivel
     * @param quantas  quantas arvores geradoras minimas existem
     * @param todas    quantas arvores geradoras existem ao todo
     * @param olhados  quantos subconjuntos foram testados
     */
    public record Bruta(long peso, long quantas, long todas, long olhados) {
    }

    /**
     * TODOS os subconjuntos de arestas, testando cada um.
     *
     * <p>Custa dois elevado ao numero de arestas, e por isso so vai ate vinte e duas.
     * E e justamente por ser burra que ela serve de juiz: ela nao sabe que o guloso
     * funciona, e se o guloso estivesse errado ela nao estaria errada junto.
     */
    public static Bruta porForcaBruta(Grafo g) {
        int m = g.arestas().size();

        if (m > 22) throw new IllegalArgumentException("a forca bruta so vai ate 22 arestas, e vieram " + m);

        long melhor = Long.MAX_VALUE;
        long quantas = 0;
        long todas = 0;
        long olhados = 0;

        for (int mascara = 0; mascara < 1 << m; mascara++) {
            if (Integer.bitCount(mascara) != g.vertices() - 1) continue;

            olhados++;

            List<Grafo.Aresta> escolhidas = new ArrayList<>(g.vertices());

            for (int i = 0; i < m; i++) if ((mascara & (1 << i)) != 0) escolhidas.add(g.arestas().get(i));

            Veredicto v = conferir(g, escolhidas);

            if (!v.vale()) continue;

            todas++;

            if (v.peso() < melhor) {
                melhor = v.peso();
                quantas = 1;
            } else if (v.peso() == melhor) {
                quantas++;
            }
        }

        return new Bruta(todas == 0 ? -1 : melhor, quantas, todas, olhados);
    }

    // ───────────────────── 3. o teorema de Kirchhoff ─────────────────────

    /**
     * O NUMERO DE ARVORES GERADORAS pelo TEOREMA DE KIRCHHOFF, de 1847.
     *
     * <p>Monte a matriz laplaciana: na diagonal, o grau de cada vertice; fora dela,
     * menos o numero de arestas entre os dois. Apague uma linha e a coluna
     * correspondente, qualquer uma. O determinante do que sobrou e o numero de arvores
     * geradoras.
     *
     * <p>Qualquer uma. Essa e a parte que parece magica: o resultado nao depende de
     * qual vertice se apaga, e a tabela confere isso apagando todos, um de cada vez.
     *
     * <p>A eliminacao e feita por BAREISS, sem fracao nenhuma: cada divisao e exata
     * porque todos os numeros intermediarios sao determinantes de submatrizes. Em
     * ponto flutuante o determinante de uma laplaciana de quinze vertices sai como
     * 2999999.9999999995, e a conferencia viraria uma discussao sobre tolerancia.
     */
    public static long quantasArvores(Grafo g) {
        return quantasArvoresApagando(g, 0);
    }

    /** O mesmo numero em inteiro grande, que nunca estoura. */
    public static java.math.BigInteger quantasArvoresGrande(Grafo g) {
        if (g.vertices() <= 1) return java.math.BigInteger.ONE;

        return determinantePorBareissGrande(menorDaLaplaciana(g, 0));
    }

    /** O menor principal da laplaciana, apagando uma linha e a coluna correspondente. */
    public static long[][] menorDaLaplaciana(Grafo g, int apagado) {
        int n = g.vertices();
        long[][] laplaciana = new long[n][n];

        for (Grafo.Aresta a : g.arestas()) {
            laplaciana[a.u()][a.u()]++;
            laplaciana[a.v()][a.v()]++;
            laplaciana[a.u()][a.v()]--;
            laplaciana[a.v()][a.u()]--;
        }

        int m = n - 1;
        long[][] menor = new long[m][m];

        for (int i = 0, li = 0; i < n; i++) {
            if (i == apagado) continue;

            for (int j = 0, lj = 0; j < n; j++) {
                if (j == apagado) continue;

                menor[li][lj++] = laplaciana[i][j];
            }

            li++;
        }

        return menor;
    }

    /** Bareiss estourou os sessenta e quatro bits num produto intermediario. */
    public static final class EstourouException extends ArithmeticException {

        public EstourouException(int coluna) {
            super("o produto intermediario de Bareiss estourou na coluna " + coluna);
        }
    }

    public static long quantasArvoresApagando(Grafo g, int apagado) {
        int n = g.vertices();

        if (n <= 1) return 1;

        long[][] laplaciana = new long[n][n];

        for (Grafo.Aresta a : g.arestas()) {
            laplaciana[a.u()][a.u()]++;
            laplaciana[a.v()][a.v()]++;
            laplaciana[a.u()][a.v()]--;
            laplaciana[a.v()][a.u()]--;
        }

        int m = n - 1;
        long[][] menor = new long[m][m];

        for (int i = 0, li = 0; i < n; i++) {
            if (i == apagado) continue;

            for (int j = 0, lj = 0; j < n; j++) {
                if (j == apagado) continue;

                menor[li][lj++] = laplaciana[i][j];
            }

            li++;
        }

        return determinantePorBareiss(menor);
    }

    /**
     * O DETERMINANTE por BAREISS: eliminacao SEM FRACAO NENHUMA.
     *
     * <p>Cada passo divide por um pivo anterior, e a divisao e sempre exata porque
     * todos os numeros intermediarios sao determinantes de submatrizes, que sao
     * inteiros.
     */
    public static long determinantePorBareiss(long[][] matriz) {
        int n = matriz.length;

        if (n == 0) return 1;

        long[][] a = new long[n][n];

        for (int i = 0; i < n; i++) a[i] = matriz[i].clone();

        long sinal = 1;
        long anterior = 1;

        for (int coluna = 0; coluna < n - 1; coluna++) {
            if (a[coluna][coluna] == 0) {
                int troca = -1;

                for (int i = coluna + 1; i < n && troca < 0; i++) if (a[i][coluna] != 0) troca = i;

                if (troca < 0) return 0;

                long[] guarda = a[coluna];

                a[coluna] = a[troca];
                a[troca] = guarda;
                sinal = -sinal;
            }

            for (int i = coluna + 1; i < n; i++)
                for (int j = coluna + 1; j < n; j++)
                    try {
                        // o QUOCIENTE sempre cabe, e o PRODUTO pode nao caber: dois
                        // determinantes de submatriz multiplicados dao o dobro de
                        // digitos, e a divisao exata so vem depois
                        a[i][j] = Math.subtractExact(
                                Math.multiplyExact(a[i][j], a[coluna][coluna]),
                                Math.multiplyExact(a[i][coluna], a[coluna][j])) / anterior;
                    } catch (ArithmeticException e) {
                        throw new EstourouException(coluna);
                    }

            anterior = a[coluna][coluna];
        }

        return sinal * a[n - 1][n - 1];
    }

    /**
     * O MESMO Bareiss em INTEIRO GRANDE, que nunca estoura.
     *
     * <p>Ele existe porque a versao de sessenta e quatro bits estoura antes do que eu
     * esperava, e nao onde eu esperava: o resultado cabe e o PRODUTO intermediario nao.
     * A tabela de Cayley mede exatamente onde isso acontece.
     */
    public static java.math.BigInteger determinantePorBareissGrande(long[][] matriz) {
        int n = matriz.length;

        if (n == 0) return java.math.BigInteger.ONE;

        java.math.BigInteger[][] a = new java.math.BigInteger[n][n];

        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) a[i][j] = java.math.BigInteger.valueOf(matriz[i][j]);

        java.math.BigInteger sinal = java.math.BigInteger.ONE;
        java.math.BigInteger anterior = java.math.BigInteger.ONE;

        for (int coluna = 0; coluna < n - 1; coluna++) {
            if (a[coluna][coluna].signum() == 0) {
                int troca = -1;

                for (int i = coluna + 1; i < n && troca < 0; i++) if (a[i][coluna].signum() != 0) troca = i;

                if (troca < 0) return java.math.BigInteger.ZERO;

                java.math.BigInteger[] guarda = a[coluna];

                a[coluna] = a[troca];
                a[troca] = guarda;
                sinal = sinal.negate();
            }

            for (int i = coluna + 1; i < n; i++)
                for (int j = coluna + 1; j < n; j++)
                    a[i][j] = a[i][j].multiply(a[coluna][coluna])
                            .subtract(a[i][coluna].multiply(a[coluna][j]))
                            .divide(anterior);

            anterior = a[coluna][coluna];
        }

        return sinal.multiply(a[n - 1][n - 1]);
    }
}
