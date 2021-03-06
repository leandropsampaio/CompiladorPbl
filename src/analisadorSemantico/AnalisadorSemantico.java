package analisadorSemantico;

import analisadorSintatico.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import semantico.Classe;
import semantico.ClasseFilha;
import semantico.Global;
import semantico.Metodo;
import semantico.Struct;
import semantico.Variavel;

/**
 *
 * @author Leandro Sampaio e Elvis Huges
 */
public class AnalisadorSemantico {

    private Token tokenAtual, tokenAnterior, tokenAnteriorAnterior;
    private Token tokenAux, proximoTokenAux;
    private int posicao = -1, posicaoAux = 0;
    private ArrayList<Token> tokens, tokensAux;
    private int errosSemanticos = 0;
    private int contador = 0, contador2 = 0;
    private String StringErrosSemanticos = null;
    private Classe classeAtual = null;
    private Metodo metodoAtual = null;
    private Variavel variavelAtual = null;
    private Variavel variavelStructAtual = null;
    private Variavel parametroAtual = null;
    private Struct structAtual = null;
    private List<Variavel> parametrosAtuais;
    private Global global = Global.getInstance();
    private FileWriter saidaSemantico;
    private boolean start = false;
    private int linhaErro;
    private String nomeVariavelAtribuicao;
    private boolean dentroDeMetodo = false;
    private boolean emExpressaoIf = false;
    private String tipoOperacao;
    private boolean relacional = false, apenasAritmetico = true;
    private String ultimaOperacao;
    private String tipo, tipo2;
    private boolean proximoToken = false;
    private boolean identificadorComsumido = false;
    private String nomeStructASerHerdade = null;

    private boolean isVetor = false;
    private boolean emScan = false;

    private boolean isFuncao = false;

    /**
     * Método que inicia a análise sintática.
     *
     * @param tokens lista de tokens extraídos da análise léxica
     * @param nomeArquivo
     */
    public void iniciar(ArrayList tokens, String nomeArquivo) throws IOException {
        saidaSemantico = new FileWriter("entrada\\saidaSemantico\\saida-" + nomeArquivo + ".txt");

        try {
            saidaSemantico.write("Análise Semântica iniciada para o arquivo " + nomeArquivo + "\n");
            saidaSemantico.write("\n");
            System.out.println("Análise Semântica iniciada para o arquivo " + nomeArquivo);
            this.tokens = tokens;
            this.tokensAux = tokens;
            proximoToken();
            this.StringErrosSemanticos = "\n";
            Iterator iterador = this.tokens.listIterator();
            programa();

            if (errosSemanticos == 0 && !proximoToken && start == true) {

                System.out.println("Análise Semântica finalizada com sucesso para o arquivo " + nomeArquivo + "\n");
                saidaSemantico.write("Análise Semântica finalizada com sucesso para o arquivo " + nomeArquivo + "\n");
            } else {
                if (start == false) {
                    saidaSemantico.write("Erro Grave: Deve existir um método start no arquivo. \n");
                }
                saidaSemantico.write(this.StringErrosSemanticos);
                saidaSemantico.write("\n\n");

                saidaSemantico.write("Análise Semântica finalizada com erro para o arquivo " + nomeArquivo);
            }
            saidaSemantico.close();

        } catch (IOException ex) {
            Logger.getLogger(AnalisadorSemantico.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /*
        Caso tenha '|' na grámatica seria if - if ...
        Caso seja direto na grámatica seria if - else if ...
     */
    private boolean proximoToken() {
        if (posicao + 1 < tokens.size()) {
            posicao++;
            tokenAnteriorAnterior = tokenAnterior;
            tokenAnterior = tokenAtual;
            tokenAtual = tokens.get(posicao);

            if (posicao + 1 < tokens.size()) {
                proximoTokenAux = tokens.get(posicao + 1);
            }

            proximoToken = true;
            return true;
        }
        proximoToken = false;
        return false;
    }

    private void tokenAnterior(int contador) {

        System.out.println("**********************************************************CONTADOR: " + contador);
        for (int i = 1; i <= contador; i++) {
            System.out.println(i);
            if (posicao - 1 < tokens.size()) {
                System.out.println("*************************************************************PASSOU!");
                posicao--;
                tokenAtual = tokens.get(posicao);
                tokenAnterior = tokens.get(posicao - 1);
                // Pulando comentários de linha e bloco
                //validarToken("Comentário de Linha");
                //validarToken("Comentário de Bloco");
            }
        }
    }

    private boolean validarToken(String tipo) {
        System.out.println("VALIDANDO TOKEN: " + tipo);

        if (tokenAtual.getTipo().equals(tipo) || tokenAtual.getNome().equals(tipo)) {
            if (emExpressaoIf) {
                Token t = new Token(tokenAtual.getNome(), tokenAtual.getTipo(), tokenAtual.getLinha());
                //tokensExpressao.add(t);
            }
            System.out.println(tokenAtual);
            proximoToken();
            return true;
        }

        //System.out.println("VALIDANDO TOKEN: " + tipo);
        return false;
    }

    private Token showProx() {
        if (posicao + 1 < tokens.size()) {
            return tokens.get(posicao + 1);
        }
        return null;
    }

    public boolean programa() {
        if (declaracao()) {
            if (programaAux()) {
                return true;
            }
        }
        return false;
    }

    public boolean programaAux() {
        if (programa()) {
            return true;
        }
        return true;
    }

    /**
     * **********************************************************************************
     * ************************* TERMINAR AS DECLARAÇÕES E VERIFICAR OS QUE
     * ********************* POSSUEM VAZIO PARA ADICIONAR O ELSE(RETURN FALSE)
     * **********************************************************************************
     */
    public boolean declaracao() {
        System.out.println("DECLARACAO");
        if (declaracaoDeFuncao()) {
            return true;
        } else if (declaracaoDeProcedimento()) {
            return true;
        } else if (declaracaoDeInicio()) {
            return true;
        } else if (declaracaoDeVar()) {
            return true;
        } else if (declaracaoDeConst()) {
            return true;
        } else if (declaracaoDeStruct()) {
            return true;
        } else if (declaracaoDeTypedef()) {
            return true;
        }
        System.out.println("SAIDA DECLARACAO");
        return false;

    }

    private boolean declaracaoDeFuncao() {
        System.out.println("DECLARACAO DE FUNCAO");
        if (validarToken("function")) {
            if (funcId()) {
                if (validarToken("(")) {
                    if (funcaoProcedimentoFim()) {
                        return true;
                    }
                }
            }
        }

        System.out.println("SAIDA DECLARACAO DE FUNCAO");
        return false;
    }

    private boolean declaracaoDeProcedimento() {
        System.out.println("DECLARACAO DE PROCEDIMENTO");
        if (validarToken("procedure")) {
            metodoAtual = new Metodo();
            System.out.println("new Metodo procedure");
            parametrosAtuais = new ArrayList();
            //System.out.println("1");
            if (validarToken("IDE")) { // adicionei esta parte !!!! 25/07
                metodoAtual.setNome(tokenAnterior.getNome());
                //System.out.println("17");
                if (validarToken("(")) {
                    if (funcaoProcedimentoFim()) {
                        return true;
                    }
                } else {
                    panicMode();
                }
            }

        }
        System.out.println("SAIDA DECLARACAO DE PROCEDIMENTO");
        return false;
    }

    private boolean declaracaoDeInicio() {
        System.out.println("DECLARACAO DE INICIO");
        if (validarToken("start")) {
            if (start) {
                System.out.println("Método Start já declarado");
                salvarMensagemArquivo("- Método Start já declarado. Linha: " + tokenAtual.getLinha());
            } else {
                start = true;
            }
            metodoAtual = new Metodo();
            parametrosAtuais = new ArrayList();
            metodoAtual.setNome("start");
            if (validarToken("(")) {
                if (validarToken(")")) {
                    addMetodo();
                    if (bloco()) {
                        metodoAtual = null;
                        return true;
                    }
                } else {
                    panicMode();
                }
            } else {
                panicMode();
            }
        }
        System.out.println("SAIDA DECLARACAO DE INICIO");
        return false;
    }

    private boolean declaracaoDeVar() {
        System.out.println("DECLARACAO DE VAR");
        if (validarToken("var")) {
            if (validarToken("{")) {
                if (declaracaoDeVariavelCorpo()) {
                    if (validarToken("}")) {
                        return true;
                    } //verificar !!!!!!
                } else {
                    panicMode();
                }
            } else {
                panicMode();
            }
        }
        System.out.println("SAIDA DECLARACAO DE VAR");
        return false;
    }

    private boolean declaracaoDeConst() {
        System.out.println("DECLARACAO DE CONST");
        if (validarToken("const")) {
            if (validarToken("{")) {
                if (declaracaoDeConstanteCorpo()) {
                    if (validarToken("}")) {
                        return true;
                    } //verificar !!!!!!
                } else {
                    panicMode();
                }
            } else {
                panicMode();
            }
        }
        System.out.println("SAIDA DECLARACAO DE CONST");
        return false;
    }

    private boolean funcId() {
        metodoAtual = new Metodo();
        System.out.println("new Metodo function");
        parametrosAtuais = new ArrayList();
        System.out.println("FUNC ID");
        if (tipo()) {
            metodoAtual.setTipo(tokenAnterior.getNome());
            metodoAtual.setNome(tokenAtual.getNome());
            if (validarToken("IDE")) {
                System.out.println("2");
                return true;
            }
        }
        System.out.println("SAIDA FUNC ID");
        return false;
    }

    private void panicMode() {
        System.out.println("Implementar modo pânico!!!!!");
    }

    private boolean tipo() {
        System.out.println("TIPO");
        if (tipobase()) {
            if (tipoAux()) {
                return true;
            }
        }
        System.out.println("SAIDA TIPO");
        return false;
    }

    /**
     * ************************** VERIFICAR A ORDEM DOS IFS
     * ******************************************
     */
    private boolean tipobase() {
        System.out.println("TIPO BASE");
        variavelAtual = new Variavel();
        if (escalar()) {
            variavelAtual.setTipo(tokenAnterior.getNome());
            return true;

        }/* else if (declaracaoDeStruct()) {
            return true;
        } */ else if (validarToken("IDE")) {
            variavelAtual.setTipo(tokenAnterior.getNome());
            System.out.println("3");
            return true;
        } else if (validarToken("struct")) {
            if (validarToken("IDE")) {
                variavelAtual.setTipo(tokenAnterior.getNome());
                VerificarExistenciaDeStruct();
                System.out.println("4");
                return true;
            } else {
                panicMode();
            }
        }
        variavelAtual = new Variavel();
        System.out.println("SAIDA TIPO BASE");
        return false;
    }

    private boolean escalar() {
        System.out.println("ESCALAR");
        if (validarToken("int") || validarToken("float") || validarToken("bool") || validarToken("string")) {
            return true;
        }
        System.out.println("SAIDA ESCALAR");
        return false;
    }

    private boolean declaracaoDeStruct() {
        System.out.println("DECLARACAO DE STRUCT");
        if (validarToken("struct")) {
            structAtual = new Struct();
            if (declaracaoDeStructAux()) {
                return true;
            }
            tokenAnterior(1);
        }
        System.out.println("SAIDA DECLARACAO DE STRUCT");
        return false;
    }

    private boolean declaracaoDeStructAux() {
        System.out.println("DECLARACAO DE STRUCT AUX");
        if (validarToken("IDE")) {
            System.out.println("123" + tokenAnterior.getNome());
            structAtual.setNome(tokenAnterior.getNome());
            structAtual.setLinha(tokenAnterior.getLinha());
            if (Extends()) { // LEMBRAR
                if (validarToken("{")) {
                    if (declaracaoDeStructCorpo()) {
                        if (validarToken("}")) {
                            addStruct();
                            verificarVariaveisHeranca();
                            System.out.println("444teste" + structAtual.getNome() + "--" + structAtual.getVariaveis());
                            structAtual = new Struct();
                            return true;
                        }
                    } else {
                        //tokenAnterior(1);
                    }
                }
            } else {
                //tokenAnterior(1);
            }
        } else if (Extends()) {
            if (validarToken("{")) {
                if (declaracaoDeStructCorpo()) {
                    if (validarToken("}")) {
                        return true;
                    }
                }
            }
        }
        System.out.println("SAIDA DECLARACAO DE STRUCT AUX");
        return false;
    }

    private boolean Extends() {
        System.out.println("EXTENDS");
        if (validarToken("extends")) {
            if (validarToken("IDE")) {
                System.out.println("structofMine " + tokenAnterior.getNome() + structAtual.getNome());
                verificarHerancaStruct();
                return true;
            } else {
                panicMode();
            }
        } // PODE SER VAZIO        
        System.out.println("SAIDA EXTENDS");
        return true;
    }

    private boolean declaracaoDeStructCorpo() {
        System.out.println("DECLARACAO DE STRUCT CORPO");
        if (declaracaoDeStructLinha()) {
            if (declaracaoDeStructCorpoAux()) {
                return true;
            }
        }
        System.out.println("SAIDA DECLARACAO DE STRUCT CORPO");
        return false;

    }

    private boolean declaracaoDeStructLinha() {
        System.out.println("DECLARACAO DE STRUCT LINHA");
        if (tipo()) {
            if (expressaoIdentificadoresStruct()) {
                System.out.println("lerolero" + structAtual.getVariaveis().size());
                structAtual.addVariavel(variavelAtual);
                return true;
            }
        }
        System.out.println("SAIDA DECLARACAO DE STRUCT LINHA");
        return false;
    }

    private boolean expressaoIdentificadoresStruct() {
        System.out.println("EXPRESSAO IDENTIFICADORES STRUCT");
        if (expressaoIdentificadorStruct()) {
            if (expressaoIdentificadoresStructAux()) {
                return true;
            }
        }
        System.out.println("SAIDA EXPRESSAO IDENTIFICADORES STRUCT");
        return false;
    }

    private boolean expressaoIdentificadorStruct() {
        if (validarToken("IDE")) {
            variavelAtual.setNome(tokenAnterior.getNome());
            System.out.println("7");
            return true;
        }
        return false;
    }

    private boolean expressaoIdentificadoresStructAux() {
        System.out.println("EXPRESSAO IDENTIFICADORES STRUCT AUX");
        if (validarToken(";")) {
            return true;
        } else if (validarToken(",")) {
            variavelAtual.setNome(tokenAnterior.getNome());
            if (expressaoIdentificadoresStruct()) {
                return true;
            }
        }
        System.out.println("SAIDA EXPRESSAO IDENTIFICADORES STRUCT AUX");
        return false;
    }

    /*
        TERMINAR!!!!!!!!
     */
    private boolean tipoAux() {
        System.out.println("TIPO AUX");
        if (tipoVetorDeclarando()) {
            return true;
        }
        System.out.println("SAIDA TIPO AUX");
        return true;
    }

    private boolean tipoVetorDeclarando() {
        System.out.println("TIPO VETOR DECLARANDO");
        if (tipoVetorDeclarado()) {
            if (tipoVetorDeclarandoAux()) {
                return true;
            }
        }
        System.out.println("SAIDA TIPO VETOR DECLARANDO");
        return false;
    }

    private boolean tipoVetorDeclarado() {
        System.out.println("TIPO VETOR DECLARADO");
        if (validarToken("[")) {
            if (validarToken("]")) {
                System.out.println("emIsVetor");
                variavelAtual.setIsVetor(true);
                return true;
            } else {
                panicMode();
            }
        }
        System.out.println("SAIDA TIPO VETOR DECLARADO");
        return false;
    }

    private boolean tipoVetorDeclarandoAux() {
        System.out.println("TIPO VETOR DECLARANDO AUX");
        if (tipoVetorDeclarando()) {
            return true;
        }
        System.out.println("SAIDA TIPO VETOR DECLARANDO AUX");
        return true;
    }

    private boolean declaracaoDeStructCorpoAux() {
        System.out.println("DECLARACAO DE STRUCT CORPO AUX");
        if (declaracaoDeStructCorpo()) {
            return true;
        }
        System.out.println("SAIDA DECLARACAO DE STRUCT CORPO AUX");
        return true;
    }

    /**
     * ***********************************************************************
     */
    private boolean funcaoProcedimentoFim() {
        System.out.println("PROCEDIMENTO FIM");
        parametroAtual = new Variavel();

        System.out.println("@@@@" + tokenAtual.getLinha());
        linhaErro = tokenAtual.getLinha();
        if (parametros()) {
            if (validarToken(")")) {
                metodoAtual.setParametros(parametrosAtuais);
                if (bloco()) {
                    addMetodo();
                    metodoAtual = new Metodo();
                    return true;
                }
            }
        } else if (validarToken(")")) {
            if (bloco()) {
                addMetodo();
                metodoAtual = new Metodo();
                return true;
            }
        }

        System.out.println("FIM PROCEDIMENTO FIM");
        return false;
    }

    private boolean parametros() {
        System.out.println("PARAMETROS");
        if (parametro()) {
            if (parametrosAux()) {
                return true;
            }
        }
        System.out.println("SAIDA PARAMETROS");
        return false;
    }

    private boolean bloco() {
        System.out.println("BLOCO");
        if (validarToken("{")) {
            if (blocoAux()) {
                //System.out.println("2");
                return true;
            }
        }
        System.out.println("SAIDA BLOCO");
        return false;
    }

    private boolean parametro() {
        System.out.println("PARAMETRO");
        if (tipo()) {
            parametroAtual.setTipo(tokenAnterior.getNome());
            if (validarToken("IDE")) {
                parametroAtual.setNome(tokenAnterior.getNome());
                //parametrosAtuais.add(parametroAtual); //addParametros
                addParametro();
                //System.out.println("8");
                return true;
            }
        }
        System.out.println("SAIDA PARAMETRO");
        return false;
    }

    private boolean parametrosAux() {
        System.out.println("PARAMETROS AUX");
        if (validarToken(",")) {
            //parametrosAtuais.add(parametroAtual); //addParametros
            if (parametros()) {
                return true;
            }
        }

        System.out.println("SAIDA PARAMETROS AUX");
        return true;
    }

    private boolean blocoAux() {
        System.out.println("BLOCO AUX");
        if (listaDeInstrucoes()) {
            if (validarToken("}")) {
                //addMetodo();
                return true;
            }
        } else if (validarToken("}")) {
            //addMetodo();//e adicionar variavel
            return true;
        }
        System.out.println("SAIDA BLOCO AUX");
        return false;
    }

    private boolean listaDeInstrucoes() {
        System.out.println("LISTA DE INSTRUCOES");
        if (instrucao()) {
            if (listaDeInstrucoesAux()) {
                return true;
            }
        }
        System.out.println("SAIDA LISTA DE INSTRUCOES");
        return false;
    }

    private boolean instrucao() {
        System.out.println("INSTRUÇÃO");
        if (instrucaoNormal()) { // provavelmente aqui
            return true;
        } else if (estruturaCondicional()) {
            return true;
        } else if (While()) {
            return true;
        } else if (declaracaoDeVar()) {
            return true;
        }// else if (declaracaoDeConst()) {
        //  return true;
        //}
        else if (declaracaoDeTypedef()) {
            return true;
        }
        System.out.println("SAIDA INSTRUCAO");
        return false;
    }

    private boolean listaDeInstrucoesAux() {
        if (listaDeInstrucoes()) {
            return true;
        }
        // Pode ser Vazio
        return true;
    }

    private boolean instrucaoNormal() {
        System.out.println("INSTRUCAO NORMAL");
        if (operacaoDeAtribuicao()) {
            if (validarToken(";")) {
                isFuncao = false;
                contador2 = 0;
                return true;
            } else {
                panicMode();
            }
        } else if (declaracaoDeStruct()) {
            if (validarToken(";")) {
                return true;
            } else {
                panicMode();
            }
        } else if (instrucaoDeRetorno()) {
            if (validarToken(";")) {
                return true;
            } else {
                panicMode();
            }
        } else if (Print()) {
            if (validarToken(";")) {
                return true;
            } else {
                panicMode();
            }
        } else if (scan()) {
            if (validarToken(";")) {
                return true;
            } else {
                panicMode();
            }
        }
        System.out.println("SAIDA INSTRUCAO NORMAL");
        return false;
    }

    /*TRÊS PRIMEIROS COMO IDENTIFICADOR*/
    private boolean operacaoDeAtribuicao() {
        System.out.println("OPERACAO DE ATRIBUICAO");
        //System.out.println("777" + tokenAnterior.getNome());
        posicaoAux = posicao;
        if (validarToken("IDE")) {

            System.out.println("LINHAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA:" + tokenAnterior.getLinha());
            //System.out.println("111111111111111111111111111111111111111111111111111111111111111111111111111");
            if (validarToken("=")) {
                nomeVariavelAtribuicao = tokenAnteriorAnterior.getNome();
                System.out.println("777nomeVariavelAtribuicao" + tokenAnterior.getNome());
                verificarDeclaracaoVariavel();
                System.out.println("777" + tokenAnteriorAnterior.getNome());
                if (global.BuscaVariavelConstantePorNome(tokenAnteriorAnterior.getNome())) {
                    System.out.println("777" + "variavel constante já declarada");
                    salvarMensagemArquivo("- Não se pode alterar o valor de uma constante. Linha: " + tokenAnteriorAnterior.getLinha());
                }
                //posicaoAux = posicao;
                if (expressao()) {
                    verificarTipoDeclaracao();
                    return true;
                }
                tokenAnterior(2);
            }
            tokenAnterior(1);
        }
        if (Final()) {
            System.out.println("2222222222222222222222222222222222222222222222222222222222222222222222222222");
            System.out.println("FINAL!");
            if (validarToken("=")) {
                if (expressao()) {
                    return true;
                }
                tokenAnterior(1);
            }
        }
        if (expressao()) {
            System.out.println("3333333333333333333333333333333333333333333333333333333333333333333333333333");
            System.out.println("EXPRESSAO!");
            return true;
        }

        System.out.println("4444444444444444444444444444444444444444444444444444444444444444444444444444444444444");
        System.out.println("SAIDA OPERACAO DE ATRIBUICAO");
        return false;
    }

    private boolean instrucaoDeRetorno() {
        System.out.println("INSTRUÇÃO DE RETORNO");
        if (validarToken("return")) {
            //tokenAux = tokensAux.get(posicao);
            posicaoAux = posicao;
            if (instrucaoDeRetornoAux()) {
                verificarTipoRetorno();
                return true;
            }
        }
        System.out.println("SAIDA INSTRUÇÃO DE RETORNO");
        return false;
    }

    private boolean Print() {
        System.out.println("PRINT");
        if (validarToken("print")) {
            if (validarToken("(")) {
                if (saida()) {
                    if (outrasSaidas()) {
                        if (validarToken(")")) {
                            return true;
                        }
                    }
                }
            }
        }
        System.out.println("SAIDA PRINT");
        return false;
    }

    private boolean scan() {
        System.out.println("SCAN");
        if (validarToken("scan")) {
            if (validarToken("(")) {
                emScan = true;
                if (entrada()) {
                    if (outrasEntradas()) {
                        if (validarToken(")")) {
                            emScan = false;
                            return true;
                        }
                    }
                }
            }
        }
        System.out.println("SAIDA SCAN");
        return false;
    }

    private boolean estruturaCondicional() {
        if (ifThen()) {
            if (estruturaCondicionalAux()) {
                return true;
            }
        }
        return false;
    }

    private boolean While() {
        System.out.println("WHILE");
        if (validarToken("while")) {
            if (validarToken("(")) {
                posicaoAux = posicao;
                if (expressao()) {
                    if (!tokenAnterior.getNome().equals("true") && !tokenAnterior.getNome().equals("false")
                            && apenasAritmetico == true) {
                        salvarMensagemArquivo("Expressão puramente com aritiméticos. Linha: " + tokenAnterior.getLinha());
                    }
                    if (validarToken(")")) {
                        if (bloco()) {
                            return true;
                        }
                    }
                }
            }
        }
        System.out.println("SAIDA WHILE");
        return false;
    }

    private boolean declaracaoDeTypedef() {
        System.out.println("DECLARACAO DE TYPEDEF");
        if (validarToken("typedef")) {
            if (declaracaoDeTypedefAux()) {
                return true;
            }
        }
        System.out.println("SAIDA DECLARACAO DE TYPEDEF");
        return false;
    }

    private boolean Final() {
        System.out.println("FINAL");
        if (validarToken("IDE")) {

            System.out.println("------------------------------------------------------------------ 4- " + contador);
            System.out.println("10");
            if (acessando()) {
                return true;
            }
            tokenAnterior(1);
        }
        System.out.println("SAIDA FINAL");
        return false;
    }

    private boolean expressao() {
        System.out.println("EXPRESSAO");
        if (opE()) {
            if (expressaoAux()) {
                return true;
            }
        }
        System.out.println("SAIDA EXPRESSAO");
        return false;
    }

    private boolean instrucaoDeRetornoAux() {
        System.out.println("INSTRUCAO DE RETORNO AUX");
        if (expressao()) {
            return true;
        }
        System.out.println("SAIDA INSTRUCAO DE RETORNO AUX");
        return true;
    }

    private boolean saida() {
        System.out.println("SAIDA");
        System.out.println("SAIDA!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        posicaoAux = posicao;
        if (expressao()) {
            System.out.println("KKKKKKKKKKKKKKK");
            verificarExpressaoPrint();
            return true;
        }
        System.out.println("SAIDA SAIDA");
        return false;
    }

    private boolean outrasSaidas() {
        System.out.println("OUTRAS SAIDAS");
        if (validarToken(",")) {
            if (saida()) {
                if (outrasSaidas()) {
                    return true;
                }
            }
        }
        System.out.println("SAIDA OUTRAS SAIDAS");
        return true;
    }

    private boolean entrada() {
        System.out.println("ENTRADAS");
        if (Final()) {
            //VerificarEntradaScanFinal();
            return true;
        } else if (validarToken("IDE")) {
            //VerificarEntradaScan();
            nomeVariavelAtribuicao = tokenAnterior.getNome();
            verificarDeclaracaoVariavel();
            //System.out.println("11");
            return true;
        }
        System.out.println("SAIDA ENTRADAS");
        return false;
    }

    private boolean outrasEntradas() {
        System.out.println("OUTRAS ENTRADAS");
        if (validarToken(",")) {
            if (entrada()) {
                if (outrasEntradas()) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
        System.out.println("SAIDA OUTRAS ENTRADAS");
        return true;
    }

    private boolean ifThen() {
        System.out.println("IF THEN");
        if (validarToken("if")) {
            //exrpressao if
            if (validarToken("(")) {
                posicaoAux = posicao;
                if (expressao()) {
                    //verificarExpressao();
                    if (!tokenAnterior.getNome().equals("true") && !tokenAnterior.getNome().equals("false")
                            && apenasAritmetico == true) {
                        salvarMensagemArquivo("Expressão puramente com aritiméticos. Linha: " + tokenAnterior.getLinha());
                    }

                    if (validarToken(")")) {
                        if (validarToken("then")) {
                            if (bloco()) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("SAIDA IF THEN");
        return false;
    }

    private boolean estruturaCondicionalAux() {
        System.out.println("ESTRUTURA CONDICIONAL AUX");
        if (validarToken("else")) {
            if (bloco()) {
                return true;
            } else {
                return false;
            }
        }
        //metodoAtual = null;
        System.out.println("SAIDA ESTRUTURA CONDICIONAL AUX");
        return true;
    }

    private boolean declaracaoDeVariavelCorpo() {
        System.out.println("DECLARACAO DE VARIAVEL CORPO");
        if (declaracaoDeVariavelLinha()) {
            if (declaracaoDeVariavelCorpoAux()) {
                return true;
            }
        }
        System.out.println("SAIDA DECLARACAO DE VARIAVEL CORPO");
        return false;
    }

    private boolean declaracaoDeVariavelLinha() {
        System.out.println("DECLARACAO DE VARIAVEL CORPO");
        if (tipo()) {
            if (expressaoIdentificadoresVar()) {
                //addVariavel();
                return true;
            }
        }
        System.out.println("SAIDA DECLARACAO DE VARIAVEL CORPO");
        return false;
    }

    private boolean declaracaoDeVariavelCorpoAux() {
        System.out.println("DECLARACAO DE VARIAVEL CORPO AUX");
        if (declaracaoDeVariavelCorpo()) {
            return true;
        }
        System.out.println("SAIDA DECLARACAO DE VARIAVEL CORPO AUX");
        return true;
    }

    private boolean expressaoIdentificadoresVar() {
        System.out.println("EXPRESSAO IDENTIFICADORES VAR");
        if (expressaoIdentificadorVar()) {
            if (expressaoIdentificadoresVarAux()) {
                return true;
            }
        }
        System.out.println("SAIDA EXPRESSAO IDENTIFICADORES VAR");
        return false;
    }

    private boolean expressaoIdentificadorVar() {
        System.out.println("EXPRESSAO IDENTIFICADOR VAR");
        if (validarToken("IDE")) {
            variavelAtual.setNome(tokenAnterior.getNome());
            System.out.println("12");
            if (expressaoIdentificadorVarAux()) {
                addVariavel();
                tipo = variavelAtual.getTipo();
                variavelAtual = new Variavel();
                variavelAtual.setTipo(tipo);
                return true;
            }

        }
        System.out.println("SAIDA EXPRESSAO IDENTIFICADOR VAR");
        return false;
    }

    private boolean expressaoIdentificadoresVarAux() {
        System.out.println("EXPRESSAO IDENTIFICADORES VAR AUX");
        if (validarToken(";")) {
            return true;
        } else if (validarToken(",")) {
            if (expressaoIdentificadoresVar()) {
                return true;
            }
        }
        System.out.println("SAIDA EXPRESSAO IDENTIFICADOR VAR AUX");
        return false;
    }

    private boolean expressaoIdentificadorVarAux() {
        System.out.println("EXPRESSAO IDENTIFICADOR VAR AUX");
        if (validarToken("=")) {
            posicaoAux = posicao;
            if (expressao()) {
                verificarTipoDeclaracao();
                return true;
            } else {
                return false;
            }
        }
        System.out.println("SAIDA EXPRESSAO IDENTIFICADOR VAR AUX");
        return true;
    }

    private boolean declaracaoDeConstanteCorpo() {
        System.out.println("DECLARACAO DE CONSTANTE CORPO");
        if (declaracaoDeConstanteLinha()) {
            if (declaracaoDeConstanteCorpoAux()) {
                return true;
            }
        }
        System.out.println("SAIDA DECLARACAO DE CONSTANTE CORPO");
        return false;
    }

    private boolean declaracaoDeConstanteLinha() {
        System.out.println("DECLARACAO DE CONSTANTE LINHA");
        if (tipo()) {
            if (expressaoIdentificadoresConst()) {
                return true;
            }
        }
        System.out.println("DECLARACAO DE CONSTANTE LINHA");
        return false;
    }

    private boolean declaracaoDeConstanteCorpoAux() {
        System.out.println("DECLARACAO DE CONSTANTE CORPO AUX");
        if (declaracaoDeConstanteCorpo()) {
            return true;
        }
        System.out.println("SAIDA DECLARACAO DE CONSTANTE CORPO AUX");
        return true;
    }

    private boolean expressaoIdentificadoresConst() {
        System.out.println("EXPRESSAO IDENTIFICADORES CONST");
        if (expressaoIdentificadorConst()) {
            addVariavel();
            tipo = variavelAtual.getTipo();
            variavelAtual = new Variavel();
            variavelAtual.setTipo(tipo);
            if (expressaoIdentificadoresConstAux()) {
                return true;
            }
        }
        System.out.println("SAIDA EXPRESSAO IDENTIFICADORES CONST");
        return false;
    }

    /* VERIFICAR TOKEN ANTERIOR NO CONST*/
    private boolean expressaoIdentificadorConst() {
        System.out.println("EXPRESSAO IDENTIFICADOR CONST");
        if (validarToken("IDE")) {
            variavelAtual.setConstante(true); // certo           
            variavelAtual.setNome(tokenAnterior.getNome());  // certo 
            //addVariavel();
            System.out.println("13");
            if (validarToken("=")) {
                posicaoAux = posicao;
                // verificarDeclaracaoVariavel();
                //System.out.println("777"+tokenAnterior.getNome());
                if (expressao()) {
                    verificarTipoDeclaracao();
                    // variavelAtual.setValor(tokenAnterior.getTipo());
                    //System.out.println("777"+tokenAnterior.getTipo());
                    return true;
                }
                //tokenAnterior(2);
            }
            //tokenAnterior(1);
        }
        System.out.println("SAIDA EXPRESSAO IDENTIFICADOR CONST");
        return false;
    }

    private boolean expressaoIdentificadoresConstAux() {
        System.out.println("EXPRESSAO IDENTIFICADORES CONST AUX");
        if (validarToken(";")) {
            return true;
        } else if (validarToken(",")) {
            if (expressaoIdentificadoresConst()) {
                return true;
            }
        }
        System.out.println("SAIDA EXPRESSAO IDENTIFICADORES CONST AUX");
        return false;
    }

    private boolean declaracaoDeTypedefAux() {
        System.out.println("DECLARACAO DE TYPEDEF AUX");
        if (tipo()) {
            if (validarToken("IDE")) {
                variavelAtual.setNome(tokenAnterior.getNome()); //typedef
                System.out.println("14");
                if (validarToken(";")) {
                    return true;
                }
            }
        }
        System.out.println("SAIDA DECLARACAO DE TYPEDEF AUX");
        return false;
    }

    private boolean acessando() {
        System.out.println("ACESSANDO");
        if (acesso()) {
            if (acessandoAux()) {
                return true;
            }
        }
        System.out.println("SAIDA ACESSANDO");
        return false;
    }

    private boolean acesso() {
        System.out.println("ACESSO");
        if (validarToken(".")) {
            //System.out.println("nomestructteste"+tokenAnteriorAnterior+tokenAtual);
            nomeVariavelAtribuicao = tokenAnteriorAnterior.getNome();
            verificarDeclaracaoVariavel();
            if (validarToken("IDE")) {
                //System.out.println("nomestructteste"+tokens.get(posicao-3)+tokens.get(posicao-1));
                verificarAcessoStruct();
                System.out.println("15");
                return true;
            }
        } else if (validarToken("[")) {
            isVetor = true;
            nomeVariavelAtribuicao = tokenAnteriorAnterior.getNome();
            System.out.println("nomeVariavelAtribuicao3" + nomeVariavelAtribuicao);
            if (!nomeVariavelAtribuicao.equals("]")) {
                verificarDeclaracaoVariavel();
            }
            verificarAcessoVetor();

            if (expressao()) {
                verificarTamanhoVetor();
                if (validarToken("]")) {
                    isVetor = false;
                    return true;
                }
            }
        }
        System.out.println("SAIDA ACESSO");
        return false;
    }

    private boolean acessandoAux() {
        System.out.println("ACESSANDO AUX");
        if (acessando()) {
            return true;
        }
        System.out.println("SAIDA ACESSANDO AUX");
        return true;
    }

    private boolean opE() {
        System.out.println("OPE");
        if (opRelacional()) {
            if (opEAux()) {
                return true;
            }
        }
        System.out.println("SAIDA OPE");
        return false;
    }

    private boolean expressaoAux() {
        System.out.println("EXPRESSAO AUX");
        if (validarToken("||")) {
            apenasAritmetico = false;
            if (expressao()) {
                return true;
            } else {
                return false;
            }
        }
        System.out.println("SAIDA EXPRESSAO AUX");
        return true;
    }

    private boolean opRelacional() {
        System.out.println("OP RELACIONAL");
        if (valorRelacional()) {
            if (opRelacionalAux()) {
                return true;
            }
        }
        System.out.println("SAIDA OP RELACIONAL");
        return false;
    }

    private boolean opEAux() {
        System.out.println("OP AUX");
        if (validarToken("&&")) {
            apenasAritmetico = false;
            if (opE()) {
                return true;
            } else {
                return false;
            }
        }
        System.out.println("SAIDA OP AUX");
        return true;
    }

    private boolean valorRelacional() {
        System.out.println("VALOR RELACIONAL");
        if (opMult()) {
            if (valorRelacionalAux()) {
                return true;
            }
        }
        System.out.println("SAIDA VALOR RELACIONAL");
        return false;
    }

    private boolean opRelacionalAux() {
        System.out.println("OP RELACIONAL AUX");
        if (escalarRelacional()) {
            if (opRelacional()) {
                return true;
            }
        }
        System.out.println("SAIDA OP RELACIONAL AUX");
        return true;
    }

    private boolean opMult() {
        System.out.println("OP MULT");
        if (opUnary()) {
            if (opMultAux()) {
                return true;
            }
        }
        System.out.println("SAIDA OP MULT");
        return false;
    }

    private boolean valorRelacionalAux() {
        System.out.println("VALOR RELACIONAL AUX");
        if (validarToken("+")) {
            if (opMult()) {
                if (valorRelacionalAux()) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else if (validarToken("-")) {
            if (opMult()) {
                if (valorRelacionalAux()) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
        System.out.println("SAIDA VALOR RELACIONAL AUX");
        return true;
    }

    private boolean escalarRelacional() {
        System.out.println("ESCALAR RELACIONAL");

        System.out.println("CAIUU!");
        if (validarToken("!=")) {
            verificarExpressaoIfWhile();
            verificarExpressao();
            posicaoAux = posicao;
            return true;
        } else if (validarToken("==")) {
            verificarExpressaoIfWhile();
            verificarExpressao();
            posicaoAux = posicao;
            return true;
        } else if (validarToken("<")) {
            verificarExpressaoIfWhile();
            verificarExpressao();
            posicaoAux = posicao;
            return true;
        } else if (validarToken("<=")) {
            verificarExpressaoIfWhile();
            verificarExpressao();
            posicaoAux = posicao;
            return true;
        } else if (validarToken(">")) {
            verificarExpressaoIfWhile();
            verificarExpressao();
            posicaoAux = posicao;
            return true;
        } else if (validarToken(">=")) {
            verificarExpressaoIfWhile();
            verificarExpressao();
            posicaoAux = posicao;
            return true;
        }

        System.out.println("PROX:" + tokenAtual.getNome());
        if (tokenAtual.getNome().equals(")")) {
            System.out.println("OKKK!");
            verificarExpressaoIfWhile();
        }
        System.out.println("SAIDA ESCALAR RELACIONAL");
        return false;
    }

    /*
     * VALOR E FINAL POSSUEM O IDENTIFICADOR COMO PRIMEIRO!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */
    private boolean opUnary() {
        System.out.println("OP UNARY");
        if (validarToken("!")) {
            if (opUnary()) {
                return true;
            }
        } else if (validarToken("++")) {
            if (opUnary()) {
                return true;
            }
        } else if (validarToken("--")) {
            if (opUnary()) {
                return true;
            }
        } else if (Final()) {
            if (simboloUnario()) {
                return true;
            }
        } else if (valor()) {
            if (simboloUnario()) {
                return true;
            }
        }
        System.out.println("SAIDA OP UNARY");
        return false;
    }

    private boolean opMultAux() {
        System.out.println("OP MULT AUX");
        if (validarToken("*")) {
            if (opUnary()) {
                if (opMultAux()) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else if (validarToken("/")) {
            if (opUnary()) {
                if (opMultAux()) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
        System.out.println("SAIDA OP MULT AUX");
        return true;
    }

    private boolean simboloUnario() {
        System.out.println("SIMBOLO UNARIO");
        if (validarToken("++")) {
            return true;
        } else if (validarToken("--")) {
            return true;
        }
        System.out.println("SAIDA SIMBOLO UNARIO");
        return true;
    }

    private boolean valor() {
        System.out.println("VALOR");
        if (validarToken("IDE")) {
            System.out.println("TOKENATUAL: " + tokenAtual);

            if (tokenAtual.getNome().equals("(")) { // É uma chamada de função
                isFuncao = true;
                System.out.println("*** 22222222222222222222222222222222222222222222222222222 ***");
                metodoAtual = new Metodo();
                metodoAtual = global.getMetodo(tokenAnterior.getNome());

                linhaErro = tokenAnterior.getLinha();
                //verificarMetodoExistente(tokenAnterior.getNome());
            } else if (isFuncao && (tokenAtual.getNome().equals(",") || tokenAtual.getNome().equals(")"))) {// É um parâmetro
                if (metodoAtual == null) {
                    salvarMensagemArquivo("- Função ou procedimento não declarado. Linha: " + linhaErro);
                } else {
                    System.out.println("*** 3333333333333333333333333333333333333333333333333333333333 ***");
                    variavelAtual = new Variavel();
                    System.out.println("MM: " + tokenAnterior.getNome());
                    nomeVariavelAtribuicao = tokenAnterior.getNome();
                    verificarDeclaracaoVariavel();
                    parametroAtual = variavelAtual;
                    System.out.println("NOMEE:" + metodoAtual.getNome());
                    //System.out.println("TIPOO: "+parametroAtual.getTipo());
                    System.out.println("PARAMETRO");
                    System.out.println("Contador2: " + contador2);
                    System.out.println("Quantida de parâmetros: " + metodoAtual.getParametros().size());
                    if (metodoAtual.getParametros().size() - 1 >= contador2) {
                        if (metodoAtual.getParametros().get(contador2) != null) {
                            variavelAtual = metodoAtual.getParametros().get(contador2);

                            if (!parametroAtual.getTipo().equals(variavelAtual.getTipo())) {
                                System.out.println("ERROOOOOOOOOOO!!!!!!!!!!!!!!!!");
                                salvarMensagemArquivo("- Chamada de função com parâmetros de tipos diferentes. Linha: " + linhaErro);
                            }
                            contador2++;
                        }
                    } else {
                        salvarMensagemArquivo("- Chamada de função com parâmetros de tipos diferentes. Linha: " + linhaErro);
                    }
                }
            } else {
                System.out.println("*** 444444444444444444444444444444444444444444444444444444444 ***");
                nomeVariavelAtribuicao = tokenAnterior.getNome();
                verificarDeclaracaoVariavel();
                if (metodoAtual != null && !metodoAtual.getNome().equals("start")) {
                    // verificarTipoRetorno();
                }

            }

            //global.BuscaVariavelConstantePorNome(tokenAnterior.getNome());
            //System.out.println("999 retorno foi" + tokenAnterior.getTipo());
            if (valorAux1()) {
                return true;
            }
            tokenAnterior(1);
        } else if (validarToken("(")) {
            if (expressao()) {
                if (validarToken(")")) {
                    return true;
                }
            }
        } else if (validarToken("NRO")) {
            if (tokenAtual.getNome().equals("(")) { // É uma chamada de função
                isFuncao = true;
                System.out.println("*** 22222222222222222222222222222222222222222222222222222 ***");
                metodoAtual = new Metodo();
                metodoAtual = global.getMetodo(tokenAnterior.getNome());

                linhaErro = tokenAnterior.getLinha();
                //verificarMetodoExistente(tokenAnterior.getNome());
            } else if (isFuncao && (tokenAtual.getNome().equals(",") || tokenAtual.getNome().equals(")"))) { // É um parâmetro

                if (metodoAtual == null) {
                    salvarMensagemArquivo("- Função ou procedimento não declarado. Linha: " + linhaErro);
                } else {
                    if (tokenAnterior.getNome().contains(".")) {
                        tipo = "float";
                    } else {
                        tipo = "int";
                    }
                    if (metodoAtual.getParametros().size() - 1 >= contador2) {
                        if (metodoAtual.getParametros().get(contador2) != null) {
                            variavelAtual = metodoAtual.getParametros().get(contador2);
                            if (!tipo.equals(variavelAtual.getTipo())) {
                                System.out.println("ERROOOOOOOOOOO!!!!!!!!!!!!!!!!");
                                salvarMensagemArquivo("- Chamada de função com parâmetros de tipos diferentes. Linha: " + linhaErro);
                            }
                            contador2++;
                        }
                    } else {
                        salvarMensagemArquivo("- Chamada de função com parâmetros de tipos diferentes. Linha: " + linhaErro);
                    }
                }
            }
            return true;
        } else if (validarToken("CAD")) {
            if (tokenAtual.getNome().equals("(")) { // É uma chamada de função
                isFuncao = true;
                System.out.println("*** 22222222222222222222222222222222222222222222222222222 ***");
                metodoAtual = new Metodo();
                metodoAtual = global.getMetodo(tokenAnterior.getNome());

                linhaErro = tokenAnterior.getLinha();
                //verificarMetodoExistente(tokenAnterior.getNome());
            } else if (isFuncao && (tokenAtual.getNome().equals(",") || tokenAtual.getNome().equals(")"))) { // É um parâmetro

                if (metodoAtual == null) {
                    salvarMensagemArquivo("- Função ou procedimento não declarado. Linha: " + linhaErro);
                } else {
                    tipo = "string";
                    if (metodoAtual.getParametros().size() - 1 >= contador2) {
                        if (metodoAtual.getParametros().get(contador2) != null) {
                            variavelAtual = metodoAtual.getParametros().get(contador2);
                            if (!tipo.equals(variavelAtual.getTipo())) {
                                System.out.println("ERROOOOOOOOOOO!!!!!!!!!!!!!!!!");
                                salvarMensagemArquivo("- Chamada de função com parâmetros de tipos diferentes. Linha: " + linhaErro);
                            }
                            contador2++;
                        }
                    } else {
                        salvarMensagemArquivo("- Chamada de função com parâmetros de tipos diferentes. Linha: " + linhaErro);
                    }
                }
            }
            return true;
        } else if (validarToken("true") || validarToken("false")) {
            if (tokenAtual.getNome().equals("(")) { // É uma chamada de função
                isFuncao = true;
                System.out.println("*** 22222222222222222222222222222222222222222222222222222 ***");
                metodoAtual = new Metodo();
                metodoAtual = global.getMetodo(tokenAnterior.getNome());

                linhaErro = tokenAnterior.getLinha();
                //verificarMetodoExistente(tokenAnterior.getNome());
            } else if (isFuncao && (tokenAtual.getNome().equals(",") || tokenAtual.getNome().equals(")"))) { // É um parâmetro

                if (metodoAtual == null) {
                    salvarMensagemArquivo("- Função ou procedimento não declarado. Linha: " + linhaErro);
                } else {
                    tipo = "bool";
                    if (metodoAtual.getParametros().size() - 1 >= contador2) {
                        if (metodoAtual.getParametros().get(contador2) != null) {
                            variavelAtual = metodoAtual.getParametros().get(contador2);
                            if (!tipo.equals(variavelAtual.getTipo())) {
                                System.out.println("ERROOOOOOOOOOO!!!!!!!!!!!!!!!!");
                                salvarMensagemArquivo("- Chamada de função com parâmetros de tipos diferentes. Linha: " + linhaErro);
                            }
                            contador2++;
                        }
                    } else {
                        salvarMensagemArquivo("- Chamada de função com parâmetros de tipos diferentes. Linha: " + linhaErro);
                    }
                }
            }
            return true;
        }
        System.out.println("SAIDA VALOR");
        return false;
    }

    private boolean valorAux1() {
        System.out.println("VALOR AUX1");
        if (validarToken("(")) {
            if (valorAux2()) {
                return true;
            } else {
                return false;
            }
        }
        System.out.println("SAIDA VALOR AUX1");
        return true;
    }

    private boolean valorAux2() {
        System.out.println("VALOR AUX 2");
        if (parametrosFuncao()) {
            if (validarToken(")")) {
                return true;
            }
        } else if (validarToken(")")) {
            return true;
        }
        System.out.println("SAIDA VALOR AUX 2");
        return false;
    }

    private boolean parametrosFuncao() {
        System.out.println("PARAMETROS FUNCAO");
        if (expressao()) {
            if (parametrosFuncaoAux()) {
                return true;
            }
        }
        System.out.println("SAIDA PARAMETROS FUNCAO");
        return false;
    }

    private boolean parametrosFuncaoAux() {
        System.out.println("PARAMETROS FUNCAO AUX");
        if (validarToken(",")) {
            if (parametrosFuncao()) {
                return true;
            } else {
                return false;
            }
        }
        System.out.println("SAIDA SPARAMETROS FUNCAO AUX");
        return true;
    }

    private void addVariavel() {

        if (metodoAtual == null) {
            if (!global.addVariavel(variavelAtual)) {
                //erro ao add variavel
                salvarMensagemArquivo("- Variável global <" + variavelAtual.getNome() + "> já existente com esse nome. Linha: " + tokenAnterior.getLinha());
            }
        } else if (!metodoAtual.addVariavel(variavelAtual)) {
            //erro ao add variavel
            salvarMensagemArquivo("- Variável <" + variavelAtual.getNome() + "> já existente com esse nome no método. Linha: " + tokenAnterior.getLinha());
        }
    }

    private void addParametro() {
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++==");
        System.out.println("******&&&&********");
        System.out.println("PARAMETRO ATUAL: " + parametroAtual.getNome());
        Iterator iterador = this.parametrosAtuais.listIterator();
        while (iterador.hasNext()) {
            Variavel variavel = (Variavel) iterador.next();
            System.out.println(variavel.getTipo());
            System.out.println(variavel.getNome());
        }
        if (!parametrosAtuais.contains(parametroAtual)) {
            System.out.println("AQUIIIIIIIIIIIIIIIIIIIIIIII");
            parametrosAtuais.add(parametroAtual);
            parametroAtual = new Variavel();
        } else {
            //erro parametro já existe com esse nome
            salvarMensagemArquivo("- Parâmetro já existente com esse nome . Linha: " + +tokenAtual.getLinha());
        }
        System.out.println("2222222222222222222222222222222222222222222");
        iterador = this.parametrosAtuais.listIterator();
        while (iterador.hasNext()) {
            Variavel variavel = (Variavel) iterador.next();
            System.out.println(variavel.getTipo());
            System.out.println(variavel.getNome());
        }
    }

    private void addMetodo() {
        if (!global.addMetodo(metodoAtual) && !metodoAtual.getNome().equals("start")) {
            //tentando declarar metodo fora da classe erro
            System.out.println("Método já foi declarado. Linha: " + linhaErro);
            salvarMensagemArquivo("- Método já foi declarado. Linha: " + linhaErro);
        }
    }

    private void verificarMetodoExistente() {
        if (metodoAtual != null) {
            System.out.println("||||||||||||||||||||||||||||||||||||||||");
            System.out.println(metodoAtual.getNome());
            System.out.println(metodoAtual.getTipo());

            Iterator iterador = this.metodoAtual.getParametros().iterator();
            while (iterador.hasNext()) {
                Variavel variavel = (Variavel) iterador.next();
                System.out.println(variavel.getTipo());
                System.out.println(variavel.getNome());
            }
            System.out.println("||||||||||||||||||||||||||||||||||||||||");
        }
        if (!global.contemMetodo(metodoAtual)) {
            //tentando declarar metodo fora da classe erro
            System.out.println("- Chamada para um método não declarado ou parâmetros errados. Linha: " + linhaErro);
            salvarMensagemArquivo("- Chamada para um método não declarado ou parâmetros errados. Linha: " + linhaErro);
        }
    }

    private void verificarTamanhoVetor() {
        String numero = tokenAnterior.getNome();
        if (numero.contains(".")) {
            System.out.println("Erro! Somente permitidos numero inteiros para tamanho de vetor");
            salvarMensagemArquivo("- Erro! Somente são permitidos números inteiros para tamanho de vetor. Linha: " + tokenAnterior.getLinha());
        } else if (tokenAnterior.getTipo().equals("IDE")) {
            // VERIFICAR SE EXISTE A VARIÁVEL
            nomeVariavelAtribuicao = tokenAnterior.getNome();
            verificarDeclaracaoVariavel();

        } else {
            int num = Integer.parseInt(numero);
            if (num < 0) {
                System.out.println("Erro! Tamanho do vetor menor que 0");
                salvarMensagemArquivo("- Erro! Tamanho do vetor menor que 0. Linha: " + tokenAnterior.getLinha());
            }
        }
    }
// criada por leandro;acessar uma que não existe . o outro é pra duas variaveis com mesmo nome

    private void verificarAcessoVetor() {

        if (variavelAtual != null) {
            if (metodoAtual != null) {
                Variavel v = metodoAtual.BuscaVariavelPorNome(nomeVariavelAtribuicao);
                if (v != null && !v.getIsVetor()) {
                    salvarMensagemArquivo("- Variável <" + nomeVariavelAtribuicao + "> não declarada como vetor. Linha: " + tokenAnterior.getLinha());
                }

            }
            if (metodoAtual == null) {
                Variavel v = global.BuscaVariavelPorNome(nomeVariavelAtribuicao);
                if (v != null && !v.getIsVetor()) {
                    salvarMensagemArquivo("- Variável <" + nomeVariavelAtribuicao + "> não declarada como vetor. Linha: " + tokenAnterior.getLinha());
                }
            }
        }

    }

    private void verificarDeclaracaoVariavel() {

        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        System.out.println("LINHA" + tokenAnterior.getLinha());
        variavelAtual = global.getVariavel(nomeVariavelAtribuicao);

        System.out.println("em declaracao variavel" + tokenAnteriorAnterior.getNome());
        System.out.println("nomeTribo" + nomeVariavelAtribuicao);

        if (variavelAtual == null) {
            System.out.println("ENTROUUUUUUUUUUU!");
            if (metodoAtual != null) {

                variavelAtual = metodoAtual.getVariavel(nomeVariavelAtribuicao, isVetor);
                if (variavelAtual == null) {
                    System.out.println("variavel não declada nesse escopo");
                    System.out.println("PASSOU!");
                    salvarMensagemArquivo("- Variável <" + nomeVariavelAtribuicao + "> não declarada nesse escopo. Linha: " + tokenAnterior.getLinha());
                }
            } else {
                System.out.println("variavel não declada nesse escopo");
                salvarMensagemArquivo("- Variável " + nomeVariavelAtribuicao + " não declarada nesse escopo. Linha: " + tokenAnterior.getLinha());
            }
        }

    }

    private void verificarTipoRetorno() {
        int posicaoInicial = posicaoAux;
        tokenAux = tokensAux.get(posicaoInicial);

        for (int i = posicaoInicial; i < posicao; i++) {
            tokenAux = tokensAux.get(i);
            System.out.println(tokenAux.getTipo());
            if (tokenAux.getTipo().equals("IDE")) {
                System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXxxx1");
                System.out.println(metodoAtual.getNome());
                Variavel variavelAux = metodoAtual.getVariavel(tokenAux.getNome(), isVetor);
                if (variavelAux != null) {
                    System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXxxx2");
                    System.out.println(variavelAux.getTipo());
                    if (!variavelAux.getTipo().equals(metodoAtual.getTipo())) {
                        salvarMensagemArquivo("- Retorno de método incompatível (" + variavelAux.getNome() + "). Linha: " + tokenAnterior.getLinha());
                    }
                }
                variavelAux = global.getVariavel(tokenAux.getNome());
                if (variavelAux != null) {
                    System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXxxx3");
                    System.out.println(variavelAux.getTipo());
                    if (!variavelAux.getTipo().equals(metodoAtual.getTipo())) {
                        salvarMensagemArquivo("- Retorno de método incompatível (" + variavelAux.getNome() + "). Linha: " + tokenAnterior.getLinha());
                    }
                }
            } else if (tokenAux.getTipo().equals("NRO")) {
                if (tokenAux.getNome().contains(".")) {
                    if (!"float".equals(metodoAtual.getTipo())) {
                        salvarMensagemArquivo("- Retorno de método incompatível (" + tokenAux.getNome() + "). Linha: " + tokenAnterior.getLinha());
                    }
                } else if (!"int".equals(metodoAtual.getTipo())) {
                    salvarMensagemArquivo("- Retorno de método incompatível (" + tokenAux.getNome() + "). Linha: " + tokenAnterior.getLinha());
                }
            } else if ((tokenAux.getTipo().equals("true") || tokenAux.getNome().equals("true")
                    && !metodoAtual.getTipo().equals("bool"))
                    || (tokenAux.getTipo().equals("false") || tokenAux.getNome().equals("false")
                    && !metodoAtual.getTipo().equals("bool"))) {
                salvarMensagemArquivo("- Retorno de método incompatível. Linha: " + tokenAnterior.getLinha());
            } else if (tokenAux.getTipo().equals("CAD")) {
                if (!"string".equals(metodoAtual.getTipo())) {
                    salvarMensagemArquivo("- Retorno de método incompatível (" + tokenAux.getNome() + "). Linha: " + tokenAnterior.getLinha());
                }
            }
        }
    }

    private void verificarTipoDeclaracao() {
        int posicaoInicial = posicaoAux;
        tokenAux = tokensAux.get(posicaoInicial);

        for (int i = posicaoInicial; i < posicao; i++) {
            tokenAux = tokensAux.get(i);
            System.out.println(tokenAux.getTipo());
            if (tokenAux.getTipo().equals("IDE")) {
                System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXxxx1");
//                System.out.println(variavelAtual.getNome());
                Variavel variavelAux = metodoAtual.getVariavel(tokenAux.getNome(), isVetor);
                if (variavelAux != null && variavelAtual != null) {
                    System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXxxx2");
                    System.out.println(variavelAux.getTipo());
                    if (!variavelAux.getTipo().equals(variavelAtual.getTipo())) {
                        salvarMensagemArquivo("- Valor incompatível com o tipo da declaração (tipos diferentes). Linha: " + tokenAnterior.getLinha());
                    }
                }
                variavelAux = global.getVariavel(tokenAux.getNome());
                if (variavelAux != null) {
                    System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXxxx3");
                    System.out.println(variavelAux.getTipo());
                    if (!variavelAux.getTipo().equals(variavelAtual.getTipo())) {
                        salvarMensagemArquivo("- Valor incompatível com o tipo da declaração (tipos diferentes). Linha: " + tokenAnterior.getLinha());
                    }
                }
            } else if (tokenAux.getTipo().equals("NRO")) {
                System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXxxx444");
                if (tokenAux.getNome().contains(".")) {
                    if (!"float".equals(variavelAtual.getTipo())) {
                        salvarMensagemArquivo("- Valor incompatível com o tipo da declaração (tipos diferentes). Linha: " + tokenAnterior.getLinha());
                    }
                } else if (!"int".equals(variavelAtual.getTipo())) {
                    salvarMensagemArquivo("- Valor incompatível com o tipo da declaração (tipos diferentes). Linha: " + tokenAnterior.getLinha());
                }
            } else if ((tokenAux.getTipo().equals("true") || tokenAux.getNome().equals("true")
                    && !variavelAtual.getTipo().equals("bool"))
                    || (tokenAux.getTipo().equals("false") || tokenAux.getNome().equals("false")
                    && !variavelAtual.getTipo().equals("bool"))) {
                salvarMensagemArquivo("- Valor incompatível com o tipo da declaração (tipos diferentes). Linha: " + tokenAnterior.getLinha());
            } else if (tokenAux.getTipo().equals("CAD")) {
                if (!"string".equals(variavelAtual.getTipo())) {
                    salvarMensagemArquivo("- Valor incompatível com o tipo da declaração (tipos diferentes). Linha: " + tokenAnterior.getLinha());
                }
            }
        }
    }

    private void addStruct() {
        System.out.println("em add struct");
        if (metodoAtual == null) {
            Struct s = global.BuscaStructPorNome(structAtual.getNome());
            if (s != null) {
                salvarMensagemArquivo("Struct <" + s.getNome() + "> já declarada. Linha: " + structAtual.getLinha() + "\n");
            } else {
                //verificarVariaveisHeranca();
                global.addStruct(structAtual);
            }
        }
        if (metodoAtual != null) {
            Struct s = metodoAtual.BuscaStructPorNome(structAtual.getNome());
            if (s != null) {
                salvarMensagemArquivo("Struct <" + s.getNome() + "> já declarada. Linha: " + structAtual.getLinha() + "\n");
            } else {
                //verificarVariaveisHeranca();
                metodoAtual.addStruct(structAtual);
                System.out.println("444mostraStruct" + metodoAtual.getStruct());
            }
        }

    }

    private void verificarAcessoStruct() {

        //System.out.println("555nomestructteste" + tokens.get(posicao - 3) + tokens.get(posicao - 1));
        //System.out.println("passou ponto");
        if (metodoAtual == null) {
            Variavel v = global.BuscaVariavelPorNome(tokens.get(posicao - 3).getNome());
            if (v == null) {
                salvarMensagemArquivo("Struct declarada como <" + tokens.get(posicao - 3).getNome() + ">  não existe no acesso. Linha: " + tokenAnterior.getLinha() + "\n");
                return;
            } else {
                //System.out.println(tokens.get(posicao - 3).getTipo() + "111minhavarlinda" + tokens.get(posicao - 1).getTipo());
                Struct s = global.BuscaStructPorNome(v.getTipo());
                System.out.println("www" + s);
                if (s != null) {
                    if (!s.BuscarVariavelPorNome(tokens.get(posicao - 1).getNome())) {
                        salvarMensagemArquivo("Variavel <" + tokens.get(posicao - 1).getNome() + ">  não existe no acesso. Linha: " + tokenAnterior.getLinha() + "\n");
                    }
                    return;
                }
            }
        }

        if (metodoAtual != null) {
            Variavel v = metodoAtual.BuscaVariavelPorNome(tokens.get(posicao - 3).getNome());
//            System.out.println(tokens.get(posicao-3).getTipo()+"minhavarlinda"+v.getTipo());
            if (v == null) {
                salvarMensagemArquivo("Struct declarada como <" + tokens.get(posicao - 3).getNome() + ">  não existe no acesso. Linha: " + tokenAnterior.getLinha() + "\n");
                return;
            } else {
                // System.out.println(tokens.get(posicao - 3).getTipo() + "111minhavarlinda" + tokens.get(posicao - 1).getTipo());
                Struct s = metodoAtual.BuscaStructPorNome(v.getTipo());
                // System.out.println("www" + s);
                if (s != null) {
                    if (!s.BuscarVariavelPorNome(tokens.get(posicao - 1).getNome())) {
                        salvarMensagemArquivo("Variavel <" + tokens.get(posicao - 1).getNome() + ">  não existe no acesso. Linha: " + tokenAnterior.getLinha() + "\n");
                    }
                    return;
                }
            }
        }

    }

    private void VerificarExistenciaDeStruct() {
        if (metodoAtual == null) {
            Struct s = global.BuscaStructPorNome(tokenAnterior.getNome());
            if (s == null) {
                salvarMensagemArquivo("Struct <" + tokenAnterior.getNome() + "> não existe. Linha: " + tokenAnterior.getLinha() + "\n");
            } else {
                //variavelAtual.setTipo(tokenAnterior.getNome());
            }
        }
        if (metodoAtual != null) {
            Struct s = metodoAtual.BuscaStructPorNome(tokenAnterior.getNome());
            if (s == null) {
                salvarMensagemArquivo("Struct <" + tokenAnterior.getNome() + "> não existe. Linha: " + tokenAnterior.getLinha() + "\n");
            } else {
                //  variavelAtual.setTipo(tokenAnterior.getNome());
            }
        }
    }

    private void verificarHerancaStruct() {

        if (metodoAtual == null) {
            if (!global.verificarHerancaStruct(tokenAnterior.getNome())) {
                salvarMensagemArquivo("Struct não existe neste escopo na declaração do extends. Linha: " + tokenAnterior.getLinha() + "\n");
            } else {
                nomeStructASerHerdade = tokenAnterior.getNome();
            }
        }
        if (metodoAtual != null) {
            if (!metodoAtual.verificarHerancaStruct(tokenAnterior.getNome())) {
                salvarMensagemArquivo("Struct não existe neste escopo na declaração do extends. Linha: " + tokenAnterior.getLinha() + "\n");
            } else {
                nomeStructASerHerdade = tokenAnterior.getNome();
            }
        }
    }

    private void verificarVariaveisHeranca() {
        if (nomeStructASerHerdade != null) {
            if (metodoAtual == null) {
                if (global.verificarVariaveisHerancas(nomeStructASerHerdade, structAtual.getNome())) {
                    salvarMensagemArquivo("Herança mal formada, atributos repetidos. Linha: " + structAtual.getLinha() + "\n");
                } else {
                    System.out.println("");
                    Struct s = global.BuscaStructPorNome(nomeStructASerHerdade);
                    System.out.println("");
                    structAtual.addListaVariaveis(s.getVariaveis());
                }
            }
            if (metodoAtual != null) {
                if (metodoAtual.verificarVariaveisHerancas(nomeStructASerHerdade, structAtual.getNome())) {
                    salvarMensagemArquivo("Herança mal formada, atributos repetidos na struct. Linha: " + structAtual.getLinha() + "\n");
                } else {
                    Struct s = metodoAtual.BuscaStructPorNome(nomeStructASerHerdade);
                    structAtual.addListaVariaveis(s.getVariaveis());
                }
            }
            this.nomeStructASerHerdade = null;
        }
    }

    private void VerificarEntradaScan() {
        if (metodoAtual == null) {
            System.out.println("tokenatual3"+tokenAnterior.getNome());
            Variavel v = global.BuscaVariavelPorNome(tokenAnterior.getNome());
            if (v == null) {
                salvarMensagemArquivo("- Variavel <" + tokenAnterior.getNome() + ">  não existe no uso do scan. Linha: " + tokenAnterior.getLinha() + "\n");
            }
        }
        if (metodoAtual != null) {
            System.out.println("tokenatua4"+tokenAnterior.getNome());
            Variavel v = metodoAtual.BuscaVariavelPorNome(tokenAnterior.getNome());
            if (v == null) {
                salvarMensagemArquivo("- Variavel<" + tokenAnterior.getNome() + "> não existe no uso do scan. Linha: " + tokenAnterior.getLinha() + "\n");
            }
        }

    }

    private void VerificarEntradaScanFinal() {

        if (emScan) {
            if (metodoAtual == null) {

                Variavel v = global.BuscaVariavelPorNome(tokenAnterior.getNome());
                if (v == null) {
                  //  salvarMensagemArquivo("- Variavel não existe no uso do scan. Linha: " + tokenAnterior.getLinha() + "\n");
                }
            }
            if (metodoAtual != null) {
                System.out.println("emscan");
                Variavel v = metodoAtual.BuscaVariavelPorNome(tokenAnterior.getNome());
                if (v == null) {
                    //  salvarMensagemArquivo("- Variavel não existe no uso do scan. Linha: " + tokenAnterior.getLinha() + "\n");
                }
            }
        }

    }

    private void verificarExpressaoPrint() {
        int posicaoInicial = posicaoAux;
        tokenAux = tokensAux.get(posicaoInicial);
        tipo = "";
        tipo2 = "";

        for (int i = posicaoInicial; i < posicao; i++) {
            tokenAux = tokensAux.get(i);
            System.out.println(tokenAux.getTipo());
            if (tokenAux.getTipo().equals("IDE")) {
                System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXxxx1");
                System.out.println(variavelAtual.getNome());
                if (metodoAtual != null) {
                    Variavel variavelAux = metodoAtual.getVariavel(tokenAux.getNome(), isVetor);
                    if (variavelAux != null) {
                        if (tipo.equals("")) {
                            tipo2 = variavelAux.getTipo();
                        }
                        tipo = variavelAux.getTipo();

                        if (!tipo.equals(tipo2)) {
                            salvarMensagemArquivo("- Expressão do print com tipos diferentes. Linha: " + tokenAnterior.getLinha());
                        }
                    }
                    variavelAux = global.getVariavel(tokenAux.getNome());
                    if (variavelAux != null) {
                        if (tipo.equals("")) {
                            tipo2 = variavelAux.getTipo();
                        }
                        tipo = variavelAux.getTipo();

                        if (!tipo.equals(tipo2)) {
                            salvarMensagemArquivo("- Expressão do print com tipos diferentes. Linha: " + tokenAnterior.getLinha());
                        }
                    }
                }
            } else if (tokenAux.getTipo().equals("NRO")) {
                System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXxxx444");
                if (tokenAux.getNome().contains(".")) {
                    if (!"float".equals(variavelAtual.getTipo())) {
                        salvarMensagemArquivo("- Expressão do print com tipos diferentes. Linha: " + tokenAnterior.getLinha());
                    }
                    tipo2 = "float";
                    return;
                } else if (!"int".equals(variavelAtual.getTipo())) {
                    salvarMensagemArquivo("- Expressão do print com tipos diferentes. Linha: " + tokenAnterior.getLinha());
                }
                tipo2 = "int";
            } else if ((tokenAux.getTipo().equals("true") || tokenAux.getNome().equals("true")
                    && !variavelAtual.getTipo().equals("bool"))
                    || (tokenAux.getTipo().equals("false") || tokenAux.getNome().equals("false")
                    && !variavelAtual.getTipo().equals("bool"))) {
                tipo2 = "bool";
                salvarMensagemArquivo("- Expressão do print com tipos diferentes. Linha: " + tokenAnterior.getLinha());
            } else if (tokenAux.getTipo().equals("CAD")) {

                if (tipo.equals("")) {
                    tipo2 = "string";
                }
                tipo = "string";
                System.out.println("SIMM!");
                System.out.println(tipo);
                System.out.println(tipo2);
                if (!tipo.equals(tipo2)) {
                    System.out.println("ENTROUUUUUU!");
                    salvarMensagemArquivo("- Expressão do print com tipos diferentes. Linha: " + tokenAnterior.getLinha());
                }
                tipo2 = "string";
            }
        }

    }

    private void verificarExpressaoIfWhile() {
        int posicaoInicial = posicaoAux;
        tokenAux = tokensAux.get(posicaoInicial);
        tipo = "";
        tipo2 = "";

        for (int i = posicaoInicial; i < posicao; i++) {
            tokenAux = tokensAux.get(i);
            System.out.println(tokenAux.getTipo());
            if (tokenAux.getTipo().equals("IDE")) {
                System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXxxx1");
                System.out.println(variavelAtual.getNome());
                System.out.println(isVetor);
                System.out.println(metodoAtual);
                if (metodoAtual != null) {
                    Variavel variavelAux = metodoAtual.getVariavel(tokenAux.getNome(), isVetor);

                    if (variavelAux != null) {
                        if (tipo.equals("")) {
                            tipo2 = variavelAux.getTipo();
                        }
                        tipo = variavelAux.getTipo();

                        if (!tipo.equals(tipo2)) {
                            salvarMensagemArquivo("- Expressão com tipos diferentes. Linha: " + tokenAnterior.getLinha());
                        }
                    }
                    variavelAux = global.getVariavel(tokenAux.getNome());
                    if (variavelAux != null) {
                        if (tipo.equals("")) {
                            tipo2 = variavelAux.getTipo();
                        }
                        tipo = variavelAux.getTipo();

                        if (!tipo.equals(tipo2)) {
                            salvarMensagemArquivo("- Expressão com tipos diferentes. Linha: " + tokenAnterior.getLinha());
                        }
                    }
                }
            } else if (tokenAux.getTipo().equals("NRO")) {
                System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXxxx444");
                if (tokenAux.getNome().contains(".")) {
                    if (!"float".equals(variavelAtual.getTipo())) {
                        salvarMensagemArquivo("- Expressão com tipos diferentes. Linha: " + tokenAnterior.getLinha());
                    }
                    tipo2 = "float";
                    return;
                } else if (!"int".equals(variavelAtual.getTipo())) {
                    salvarMensagemArquivo("- Expressão com tipos diferentes. Linha: " + tokenAnterior.getLinha());
                }
                tipo2 = "int";
            } else if ((tokenAux.getTipo().equals("true") || tokenAux.getNome().equals("true")
                    && !variavelAtual.getTipo().equals("bool"))
                    || (tokenAux.getTipo().equals("false") || tokenAux.getNome().equals("false")
                    && !variavelAtual.getTipo().equals("bool"))) {
                tipo2 = "bool";
                salvarMensagemArquivo("- Expressão com tipos diferentes. Linha: " + tokenAnterior.getLinha());
            } else if (tokenAux.getTipo().equals("CAD")) {

                if (tipo.equals("")) {
                    tipo2 = "string";
                }
                tipo = "string";
                System.out.println("SIMM!");
                System.out.println(tipo);
                System.out.println(tipo2);
                if (!tipo.equals(tipo2)) {
                    System.out.println("ENTROUUUUUU!");
                    salvarMensagemArquivo("- Expressão com tipos diferentes. Linha: " + tokenAnterior.getLinha());
                }
                tipo2 = "string";
            }
        }
    }

    private void verificarExpressao() {
        apenasAritmetico = false;
        //tokenAnterior tokenAtual tokenProximo
        System.out.println("TOKENANTERIOR:" + tokenAnteriorAnterior);
        System.out.println("TOKENATUAL:" + tokenAnterior);
        System.out.println("PROXIMOTOKEN:" + tokenAtual);
        if (tokenAnterior.getTipo().equals("REL")) {
            System.out.println("RELACIONAL!*************");
            if (relacional == true) {
                salvarMensagemArquivo("- Expressão incorreta. Linha: " + tokenAnterior.getLinha());
            }
            relacional = true;
        }

        if (tokenAnteriorAnterior.getTipo().equals("IDE")) {
            nomeVariavelAtribuicao = tokenAnteriorAnterior.getNome();
            verificarDeclaracaoVariavel();
            if (variavelAtual == null) {
                salvarMensagemArquivo("- Variável <" + nomeVariavelAtribuicao + "> não declarada no escopo. Linha: " + tokenAnterior.getLinha());
                return;
            }
            tipoOperacao = variavelAtual.getTipo();
        } else if (tokenAnteriorAnterior.getTipo().equals("NRO")) {
            if (tokenAnteriorAnterior.getNome().contains(".")) {
                tipoOperacao = "float";
            } else {
                tipoOperacao = "int";
            }
        } else if (tokenAnteriorAnterior.getTipo().equals("PRE")) {
            salvarMensagemArquivo("- Expressão incorreta. Linha: " + tokenAnterior.getLinha());
            return;
        }

        if (tokenAtual.getTipo().equals("IDE")) {
            nomeVariavelAtribuicao = tokenAnteriorAnterior.getNome();
            verificarDeclaracaoVariavel();
            if (variavelAtual == null) {
                salvarMensagemArquivo("- Variável <" + nomeVariavelAtribuicao + "> não declarada no escopo. Linha: " + tokenAnterior.getLinha());
                return;
            }
            if (!variavelAtual.getTipo().equals(tipoOperacao)) {
                salvarMensagemArquivo("- Expressão incorreta. Linha: " + tokenAnterior.getLinha());
            }
        } else if (tokenAtual.getTipo().equals("NRO")) {
            if (tokenAtual.getNome().contains(".")) {
                if (!tipoOperacao.equals("float")) {
                    salvarMensagemArquivo("- Expressão incorreta. Linha: " + tokenAnterior.getLinha());
                }
            } else if (!tipoOperacao.equals("int")) {
                salvarMensagemArquivo("- Expressão incorreta. Linha: " + tokenAnterior.getLinha());
            }
            return;
        }
        salvarMensagemArquivo("- Expressão incorreta. Linha: " + tokenAnterior.getLinha());
    }

    private void salvarMensagemArquivo(String mensagem) {
        try {
            saidaSemantico.write(mensagem + "\n");
            errosSemanticos++;
        } catch (IOException ex) {
            Logger.getLogger(AnalisadorSemantico.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
