# arvore-geradora

Árvore geradora mínima do zero em Java 21: Kruskal, Prim e Borůvka, com conjuntos
disjuntos escritos à mão.

O juiz é a **força bruta**: todos os subconjuntos de arestas, filtrando os que são
árvore geradora e pegando o de menor peso. Ela não sabe que o guloso funciona, e se
o guloso estivesse errado ela não estaria errada junto.

E há um segundo juiz que não olha para aresta nenhuma uma por uma: o **teorema de
Kirchhoff**, de 1847. O número de árvores geradoras de um grafo é o determinante de
qualquer menor principal da matriz laplaciana.

Qualquer. Essa é a parte que parece mágica: o resultado não depende de qual vértice
se apaga, e a tabela confere isso apagando todos.

```
$ mvn exec:java -Dexec.args="kirchhoff"

  grafo                       vertices   arestas      por Kirchhoff   pela forca bruta   batem  mesmo apagando qualquer
  ciclo de 8                         8         8                  8                  8     sim                      sim
  estrela com atalho de 9            9        15                987                987     sim                      sim
  completo de 7 ate 3                7        21              16807              16807     sim                      sim
  grade 4x3                         12        17               2415               2415     sim                      sim
  sorteado de 10 a 0.4 (6)          10        26             452714           nao cabe     sim                      sim
  desconexo de 8                     8         6                  0                  0     sim                      sim
```

Contar árvores geradoras poderia ser um problema de contagem difícil, como contar
extensões lineares ou calcular uma permanente, e não é: é um determinante, em tempo
polinomial.

## A versão que parece certa

"Recuse a aresta se os dois vértices dela já foram visitados" soa como "recuse se
ela fecha ciclo". Não é.

```
$ mvn exec:java -Dexec.args="erro"

  grafo                       vertices  arestas que deu   peso certo peso da versao    vale motivo
  ciclo de 8                         8                7           28             28     sim
  estrela com atalho de 9            9                8          107            107     sim
  completo de 6 ate 20               6                4           15              9     NAO tem 4 arestas e precisaria de 5
  grade 4x3                         12                8          275            175     NAO tem 8 arestas e precisaria de 11
  sorteado de 9 a 0.3 (5)            9                6           13              7     NAO tem 6 arestas e precisaria de 8
```

Dois vértices já visitados podem estar em **pedaços separados** da floresta, e a
aresta entre eles é justamente a que junta os dois. Ela recusa, e termina com menos
de n menos uma arestas sem perceber.

```
  familia                      grafos  Kruskal errou Prim errou Boruvka errou visitados errou
  sorteados de 12 a 0.1           200              0          0            0            197
  sorteados de 12 a 0.4           200              0          0            0            194
  sorteados de 12 a 0.7           200              0          0            0            189
```

## Como rodar

```
mvn test
mvn exec:java -Dexec.args="tudo"
```

Os comandos do medidor são `definicao`, `erro`, `kirchhoff`, `empates`, `custo` e
`tudo`.

## O que as medidas me corrigiram

**O desempate que eu pus por reprodutibilidade apagou o fenômeno que eu queria
mostrar.** A tabela de empates ia mostrar os três algoritmos dando árvores
diferentes com o mesmo peso. Ela mostra os três dando a **mesma** árvore em todas as
doze linhas, inclusive num grafo com 27 árvores mínimas.

O motivo é que eu ordenei as arestas por peso e, no empate, pelos vértices. Isso não
é um detalhe de ordenação: é uma **ordem total** nas arestas, e com ordem total a
árvore geradora mínima é única. É teorema.

```
$ mvn exec:java -Dexec.args="empates"

  grafo                         arvores minimas   com desempate: a mesma   sem desempate: a mesma       peso peso ao contrario
  ciclo de 5                                  1                      sim                      sim         10               10
  ciclo uniforme de 5                         5                      sim                      NAO          4                4
  completo de 7 ate 3                        27                      sim                      NAO          7                7
  grade 3x3                                   3                      sim                      NAO         39               39
```

A tabela que ficou é melhor que a que eu queria. Ela escreve o mesmo grafo com as
arestas na ordem inversa e compara: com desempate, sai a mesma árvore; sem
desempate, sai outra, do mesmo peso, sempre que houver mais de uma árvore mínima.

E o peso é idêntico nas quatro colunas, em toda linha. É por isso que comparar
**peso** é certo e comparar **árvore** é errado.

**Bareiss estoura os 64 bits muito antes de a resposta deixar de caber.** A
eliminação de Bareiss não usa fração nenhuma: cada divisão é exata porque todos os
números intermediários são determinantes de submatrizes. Eu a escolhi justamente por
isso, achando que ela me livrava de pensar em tamanho.

```
$ mvn exec:java -Dexec.args="kirchhoff"

   vertices         em 64 bits        em inteiro grande               por Cayley   coube em 64 bits    batem
         11         2357947691               2357947691               2357947691                sim      sim
         12           estourou              61917364224              61917364224                NAO      sim
         17           estourou      2862423051509815793      2862423051509815793                NAO      sim
         18           estourou    121439531096594251776    121439531096594251776                NAO      sim
```

Com 12 vértices a resposta tem **onze dígitos** e um `long` aguenta dezenove. A
conta estoura assim mesmo, porque ela multiplica dois determinantes de submatriz
**antes** de dividir, e o produto tem o dobro dos dígitos do quociente.

Com 17 vértices a resposta ainda cabe num `long`, e a conta já não cabe há cinco
tamanhos. A versão em inteiro grande existe por causa dessa medida, e a de 64 bits
ficou com uma detecção explícita de estouro em vez de devolver um número errado em
silêncio.

**A fórmula de Cayley é o juiz que não custa nada.** O grafo completo de n vértices
tem n elevado a n menos dois árvores geradoras, e isso é de 1889. É um número
fechado: sem matriz, sem grafo, sem percorrer nada.

Foi ele que pegou o estouro do Bareiss. Kirchhoff e a força bruta concordavam
porque a força bruta não chega a doze vértices, e as duas teriam ficado concordando
num intervalo onde nenhuma das duas estava sendo testada.

## Licença

MIT.
