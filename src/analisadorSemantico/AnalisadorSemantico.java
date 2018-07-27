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
import semantico.Variavel;

/**
 *
 * @author Leandro Sampaio e Elvis Huges
 */
public class AnalisadorSemantico {

    private Token tokenAtual, tokenAnterior;
    private int posicao = -1;
    private ArrayList<Token> tokens;
    private int errosSemanticos = 0;
    private int contador = 0;
    private String StringErrosSemanticos = null;
    private Classe classeAtual = null;
    private Metodo metodoAtual = null;
    private Variavel variavelAtual = null;
    private Variavel parametroAtual = null;
    private List<Variavel> parametrosAtuais;
    private Global global = Global.getInstance();
    private FileWriter saidaSemantico;
    private boolean start = false;
    private int linhaErro;

    private boolean proximoToken = false;

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

            System.out.println("Análise Semântica iniciada para o arquivo " + nomeArquivo);
            this.tokens = tokens;
            proximoToken();
            this.StringErrosSemanticos = "\n";
            Iterator iterador = this.tokens.listIterator();
            programa();

            if (errosSemanticos == 0 && !proximoToken && start == true) {

                System.out.println("Análise Semântica finalizada com sucesso para o arquivo " + nomeArquivo + "\n");
                saidaSemantico.write("Análise Semântica finalizada com sucesso para o arquivo " + nomeArquivo + "\n");
            } else {

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
            tokenAnterior = tokenAtual;
            tokenAtual = tokens.get(posicao);
            // Pulando comentários de linha e bloco
            //validarToken("Comentário de Linha");
            //validarToken("Comentário de Bloco");
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
        //System.out.println("TESTE!");
        if (tokenAtual.getTipo().equals(tipo) || tokenAtual.getNome().equals(tipo)) {
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
                salvarMensagemArquivo("Erro! Método Start já declarado. Linha: " + tokenAtual.getLinha() + "\n");
            } else {
                start = true;
            }

            if (validarToken("(")) {
                if (validarToken(")")) {
                    if (bloco()) {
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
        } else if (declaracaoDeStruct()) {
            return true;
        } else if (validarToken("IDE")) {
            variavelAtual.setTipo(tokenAnterior.getNome());
            System.out.println("3");
            return true;
        } else if (validarToken("struct")) {
            if (validarToken("IDE")) {
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
            if (Extends()) { // LEMBRARRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR
                if (validarToken("{")) {
                    if (declaracaoDeStructCorpo()) {
                        if (validarToken("}")) {
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
                //System.out.println("6");
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
                    metodoAtual = null;
                    return true;
                }
            }
        } else if (validarToken(")")) {
            if (bloco()) {
                addMetodo();
                metodoAtual = null;
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
        if (instrucaoNormal()) {
            return true;
        } else if (estruturaCondicional()) {
            return true;
        } else if (While()) {
            return true;
        } else if (declaracaoDeVar()) {
            return true;
        } else if (declaracaoDeConst()) {
            return true;
        } else if (declaracaoDeTypedef()) {
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
        if (validarToken("IDE")) {
            System.out.println("111111111111111111111111111111111111111111111111111111111111111111111111111");
            if (validarToken("=")) {
                if (expressao()) {
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
            if (instrucaoDeRetornoAux()) {
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
                if (entrada()) {
                    if (outrasEntradas()) {
                        if (validarToken(")")) {
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
                if (expressao()) {
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
        if (expressao()) {
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
            return true;
        } else if (validarToken("IDE")) {
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
            if (validarToken("(")) {
                if (expressao()) {
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
        metodoAtual = null;
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
                addVariavel();                
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
            if (expressao()) {
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
            variavelAtual.setConstante(true);
            variavelAtual.setNome(tokenAnterior.getNome());
            System.out.println("13");
            if (validarToken("=")) {
                if (expressao()) {
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
            if (validarToken("IDE")) {
                System.out.println("15");
                return true;
            }
        } else if (validarToken("[")) {
            if (expressao()) {
                if (validarToken("]")) {
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
        if (validarToken("!=")) {
            return true;
        } else if (validarToken("==")) {
            return true;
        } else if (validarToken("<")) {
            return true;
        } else if (validarToken("<=")) {
            return true;
        } else if (validarToken(">")) {
            return true;
        } else if (validarToken(">=")) {
            return true;
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
            System.out.println("16");
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
            return true;
        } else if (validarToken("CAD")) {
            return true;
        } else if (validarToken("true")) {
            return true;
        } else if (validarToken("false")) {
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
                salvarMensagemArquivo("Variável global já existente com esse nome. Linha: " + tokenAnterior.getLinha());
            }
        } else if (!metodoAtual.addVariavel(variavelAtual)) {
            //erro ao add variavel
            salvarMensagemArquivo("Variável já existente com esse nome no metodo. Linha: " + tokenAnterior.getLinha());
        }
    }

    private void addParametro() {
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++==");
        System.out.println("******&&&&********");
        System.out.println("PARAMETRO ATUAL: "+ parametroAtual.getNome());
        Iterator iterador = this.parametrosAtuais.listIterator();
        while (iterador.hasNext()) {
            Variavel variavel = (Variavel) iterador.next();
            System.out.println(variavel.getTipo());
            System.out.println(variavel.getNome());
        }
        if (!parametrosAtuais.contains(parametroAtual)) {
            System.out.println("AQUIIIIIIIIIIIIIIIIIIIIIIII");
            parametrosAtuais.add(parametroAtual);
        } else {
            //erro parametro já existe com esse nome
            salvarMensagemArquivo("Parâmetro já existente com esse nome . Linha: " + +tokenAtual.getLinha());
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
        if (!global.addMetodo(metodoAtual)) {
            //tentando declarar metodo fora da classe erro
            System.out.println("Método já foi declarado. Linha: " + linhaErro);
            salvarMensagemArquivo("Método já foi declarado. Linha: " + linhaErro);
        }
    }

    private void verificarHeranca() {
        List<Classe> classes = global.getClasses();
        for (Classe c : classes) {
            if (c instanceof ClasseFilha) {
                Classe mae = global.getClasse(((ClasseFilha) c).getNomeMae());
                if (mae != null) {
                    if (mae instanceof ClasseFilha) {
                        System.out.println("erro não permitida herança em cadeia");
                        salvarMensagemArquivo("Erro! Herança em cadeia não permitida. Linha: " + +tokenAtual.getLinha());
                    } else {
                        ((ClasseFilha) c).setMae(mae);
                    }
                } else {
                    System.out.println("mae não encontrada");
                    salvarMensagemArquivo("Erro! Classe Mãe não encontrada. Linha " + tokenAtual.getLinha());
                }
            }
        }
    }

    private void verificarTamanhoVetor() {
        String numero = tokenAnterior.getNome();
        if (numero.contains(".")) {
            System.out.println("Erro! Somente permitidos numero inteiros para tamanho de vetor");
            salvarMensagemArquivo("Erro! Somente são permitidos números inteiros para tamanho de vetor. Linha: " + tokenAnterior.getLinha());
        } //removeu o else
    }

    private void salvarMensagemArquivo(String mensagem) {
        try {
            saidaSemantico.write(mensagem);
            errosSemanticos++;
        } catch (IOException ex) {
            Logger.getLogger(AnalisadorSemantico.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
