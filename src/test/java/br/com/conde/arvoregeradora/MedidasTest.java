package br.com.conde.arvoregeradora;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os testes das TABELAS que o medidor imprime.
 *
 * <p>Toda frase do README que e um numero esta aqui como afirmacao. Texto nao quebra
 * quando deixa de ser verdade; teste quebra.
 */
class MedidasTest {

    @Test
    @DisplayName("os tres batem com a forca bruta em toda linha")
    void todaLinhaBate() {
        List<Medidas.LinhaDefinicao> linhas = Medidas.contraAForcaBruta();

        assertThat(linhas).hasSize(13);
        assertThat(linhas).allSatisfy(l -> assertThat(l.todosBatem()).as(l.grafo()).isTrue());

        // e no grafo desconexo os tres dizem que nao ligaram
        Medidas.LinhaDefinicao desconexo = linhas.stream()
                .filter(l -> !l.conexo())
                .findFirst()
                .orElseThrow();

        for (String nome : List.of("Kruskal", "Prim", "Boruvka"))
            assertThat(desconexo.pesos().get(nome)).as(nome).isEqualTo(-1);
    }

    @Test
    @DisplayName("a versao com visitados erra em quase todo grafo sorteado")
    void aVersaoComVisitadosErra() {
        List<Medidas.LinhaCaca> caca = Medidas.cacarErros();

        assertThat(caca).hasSize(4);
        assertThat(caca).allSatisfy(l -> {
            assertThat(l.kruskalErrou()).as(l.familia()).isZero();
            assertThat(l.primErrou()).as(l.familia()).isZero();
            assertThat(l.boruvkaErrou()).as(l.familia()).isZero();
            assertThat(l.visitadosErrou()).as(l.familia()).isGreaterThan(180);
        });
    }

    @Test
    @DisplayName("Kirchhoff bate com a forca bruta e nao depende do vertice apagado")
    void kirchhoffNaTabela() {
        List<Medidas.LinhaKirchhoff> linhas = Medidas.oTeoremaDeKirchhoff();

        assertThat(linhas).hasSize(13);
        assertThat(linhas).allSatisfy(l -> {
            assertThat(l.batem()).as(l.grafo()).isTrue();
            assertThat(l.mesmoApagandoQualquer()).as(l.grafo()).isTrue();
        });

        // o ciclo de n tem n arvores, e o completo de 6 tem 6 elevado a 4
        assertThat(achar(linhas, "ciclo de 8").porKirchhoff()).isEqualTo(8);
        assertThat(achar(linhas, "completo de 6 ate 20").porKirchhoff()).isEqualTo(1296);
        assertThat(achar(linhas, "desconexo de 8").porKirchhoff()).isZero();
    }

    /**
     * O ACHADO QUE ME CORRIGIU: Bareiss em sessenta e quatro bits estoura com doze
     * vertices, onde a resposta tem onze digitos e caberia com folga.
     */
    @Test
    @DisplayName("Bareiss estoura em 64 bits a partir de doze vertices, e o inteiro grande nao")
    void oEstouroNaTabela() {
        List<Medidas.LinhaCompleto> linhas = Medidas.aFormulaDeCayley();

        assertThat(linhas).hasSize(17);
        assertThat(linhas).allSatisfy(l -> assertThat(l.batem()).as("%d vertices", l.vertices()).isTrue());

        for (Medidas.LinhaCompleto l : linhas)
            assertThat(l.coubeEmSessentaEQuatro()).as("%d vertices", l.vertices()).isEqualTo(l.vertices() <= 11);

        Medidas.LinhaCompleto doze = linhas.stream().filter(l -> l.vertices() == 12).findFirst().orElseThrow();

        assertThat(doze.porKirchhoff()).isEqualTo("estourou");
        assertThat(doze.emInteiroGrande()).isEqualTo("61917364224");

        // a resposta tem onze digitos e o long aguenta dezenove
        assertThat(doze.emInteiroGrande()).hasSize(11);
    }

    /**
     * E o OUTRO achado: o desempate que eu pus por reprodutibilidade tornou a arvore
     * minima unica.
     */
    @Test
    @DisplayName("com desempate a arvore nao muda com a ordem de escrita, e sem desempate muda")
    void oDesempateNaTabela() {
        List<Medidas.LinhaDesempate> linhas = Medidas.oQueODesempateFaz();

        assertThat(linhas).hasSize(12);

        // com desempate nunca muda
        assertThat(linhas).allSatisfy(l -> assertThat(l.comDesempateDaAMesma()).as(l.grafo()).isTrue());

        // e o peso e o mesmo em toda linha, com ou sem
        assertThat(linhas).allSatisfy(l -> assertThat(l.peso()).as(l.grafo()).isEqualTo(l.pesoAoContrario()));

        // sem desempate, muda sempre que ha mais de uma arvore minima
        for (Medidas.LinhaDesempate l : linhas)
            if (l.arvoresMinimas() == 1)
                assertThat(l.semDesempateDaAMesma()).as(l.grafo()).isTrue();

        assertThat(linhas.stream().filter(l -> !l.semDesempateDaAMesma()).count()).isGreaterThan(5);
    }

    @Test
    @DisplayName("os tres dao a mesma arvore em toda linha da tabela de empates")
    void osEmpatesNaTabela() {
        List<Medidas.LinhaEmpate> linhas = Medidas.osEmpates();

        assertThat(linhas).hasSize(12);
        assertThat(linhas).allSatisfy(l -> assertThat(l.asTresDaoAMesmaArvore()).as(l.grafo()).isTrue());

        // com pesos todos distintos a arvore minima e unica, e e teorema
        for (Medidas.LinhaEmpate l : linhas)
            if (l.pesosDistintos() == l.arestas() && l.arvoresMinimas() >= 0)
                assertThat(l.arvoresMinimas()).as(l.grafo()).isEqualTo(1);
    }

    @Test
    @DisplayName("Prim gasta mais passos que Kruskal no grafo completo")
    void oCrescimentoNaTabela() {
        List<Medidas.LinhaCrescimento> linhas = Medidas.oCrescimento();

        assertThat(linhas).hasSize(7);

        for (Medidas.LinhaCrescimento l : linhas)
            assertThat(l.arestas()).as("%d vertices", l.vertices()).isEqualTo(l.vertices() * (l.vertices() - 1) / 2);

        // os tres crescem com o numero de arestas
        for (int i = 1; i < linhas.size(); i++) {
            assertThat(linhas.get(i).kruskal()).isGreaterThan(linhas.get(i - 1).kruskal());
            assertThat(linhas.get(i).prim()).isGreaterThan(linhas.get(i - 1).prim());
            assertThat(linhas.get(i).boruvka()).isGreaterThan(linhas.get(i - 1).boruvka());
        }
    }

    private static Medidas.LinhaKirchhoff achar(List<Medidas.LinhaKirchhoff> linhas, String grafo) {
        return linhas.stream().filter(l -> l.grafo().equals(grafo)).findFirst().orElseThrow();
    }
}
