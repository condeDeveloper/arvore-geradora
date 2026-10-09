package br.com.conde.arvoregeradora;

import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O TEOREMA DE KIRCHHOFF, de 1847: o numero de arvores geradoras de um grafo e o
 * determinante de qualquer menor principal da matriz laplaciana.
 *
 * <p>Contar arvores geradoras poderia ser um problema de contagem dificil, como
 * contar extensoes lineares ou calcular uma permanente, e nao e: e um determinante,
 * em tempo polinomial.
 */
class JuizTest {

    @Test
    @DisplayName("Kirchhoff bate com a contagem por forca bruta em todo grafo pequeno")
    void kirchhoffBateComAForcaBruta() {
        for (Grafo g : Grafo.todos().values()) {
            if (g.arestas().size() > 22) continue;

            assertThat(Juiz.quantasArvores(g)).as(g.nome()).isEqualTo(Juiz.porForcaBruta(g).todas());
        }
    }

    /**
     * E da o MESMO numero qualquer que seja o vertice apagado.
     *
     * <p>Isso nao e obvio nenhum: a matriz muda, o determinante nao.
     */
    @Test
    @DisplayName("o determinante nao depende de qual vertice se apaga")
    void naoDependeDeQualVerticeSeApaga() {
        for (Grafo g : Grafo.todos().values()) {
            long esperado = Juiz.quantasArvores(g);

            for (int apagado = 0; apagado < g.vertices(); apagado++)
                assertThat(Juiz.quantasArvoresApagando(g, apagado))
                        .as("%s apagando %d", g.nome(), apagado)
                        .isEqualTo(esperado);
        }
    }

    /**
     * A FORMULA DE CAYLEY, de 1889: o grafo completo de n vertices tem n elevado a n
     * menos dois arvores geradoras.
     *
     * <p>E um quarto juiz: um numero fechado, sem matriz, sem grafo, sem nada.
     */
    @Test
    @DisplayName("no grafo completo, Kirchhoff da a formula de Cayley")
    void aFormulaDeCayley() {
        for (int n = 2; n <= 20; n++) {
            BigInteger cayley = BigInteger.valueOf(n).pow(n - 2);

            assertThat(Juiz.quantasArvoresGrande(Grafo.completo(n, 1, 1)))
                    .as("completo de %d", n)
                    .isEqualTo(cayley);
        }
    }

    @Test
    @DisplayName("o ciclo de n tem exatamente n arvores geradoras")
    void oCicloTemNArvores() {
        for (int n = 3; n <= 15; n++)
            assertThat(Juiz.quantasArvores(Grafo.ciclo(n))).as("ciclo de %d", n).isEqualTo(n);
    }

    @Test
    @DisplayName("um grafo desconexo tem zero arvores geradoras")
    void desconexoDaZero() {
        for (int porPedaco : new int[] {2, 4, 8})
            assertThat(Juiz.quantasArvores(Grafo.desconexo(porPedaco))).isZero();
    }

    /**
     * O ACHADO QUE ME CORRIGIU: Bareiss estoura os sessenta e quatro bits muito antes
     * de a RESPOSTA deixar de caber.
     *
     * <p>Com doze vertices o numero de arvores do completo e 61.917.364.224, que cabe
     * num long com folga de oito ordens de grandeza. A eliminacao estoura assim mesmo,
     * porque ela multiplica dois determinantes de submatriz ANTES de dividir, e o
     * produto tem o dobro dos digitos do quociente.
     *
     * <p>Com dezessete vertices a resposta ainda cabe num long, e a conta ja nao cabe
     * ha cinco tamanhos.
     */
    @Test
    @DisplayName("Bareiss estoura em 64 bits muito antes de a resposta deixar de caber")
    void bareissEstouraAntesDaResposta() {
        // ate onze vertices a conta cabe
        assertThat(Juiz.quantasArvores(Grafo.completo(11, 1, 1))).isEqualTo(2_357_947_691L);

        // com doze a RESPOSTA cabe num long e a CONTA nao
        BigInteger resposta = Juiz.quantasArvoresGrande(Grafo.completo(12, 1, 1));

        assertThat(resposta).isEqualTo(BigInteger.valueOf(61_917_364_224L));
        assertThat(resposta.bitLength()).isLessThan(63);

        assertThatThrownBy(() -> Juiz.quantasArvores(Grafo.completo(12, 1, 1)))
                .isInstanceOf(Juiz.EstourouException.class)
                .hasMessageContaining("estourou");

        // e com dezessete a resposta ainda cabe
        assertThat(Juiz.quantasArvoresGrande(Grafo.completo(17, 1, 1)).bitLength()).isLessThan(63);
    }

    @Test
    @DisplayName("as duas versoes de Bareiss dao o mesmo onde as duas cabem")
    void asDuasVersoesConcordam() {
        for (Grafo g : Grafo.todos().values())
            assertThat(BigInteger.valueOf(Juiz.quantasArvores(g)))
                    .as(g.nome())
                    .isEqualTo(Juiz.quantasArvoresGrande(g));
    }

    @Test
    @DisplayName("a definicao recusa conjunto com ciclo, com vertice de fora e com aresta de fora")
    void aDefinicaoRecusa() {
        Grafo g = Grafo.ciclo(4);
        List<Grafo.Aresta> arestas = g.arestas();

        assertThat(Juiz.conferir(g, arestas).motivo()).contains("precisaria de");
        assertThat(Juiz.conferir(g, List.of(arestas.get(0), arestas.get(1), arestas.get(2))).vale()).isTrue();

        Grafo outro = Grafo.ciclo(5);

        assertThat(Juiz.conferir(g, List.of(arestas.get(0), arestas.get(1), outro.arestas().get(4))).motivo())
                .contains("nao esta no grafo");
    }

    @Test
    @DisplayName("a forca bruta se recusa a tentar acima de vinte e duas arestas")
    void aForcaBrutaTemTeto() {
        assertThatThrownBy(() -> Juiz.porForcaBruta(Grafo.completo(8, 5, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("22");
    }
}
