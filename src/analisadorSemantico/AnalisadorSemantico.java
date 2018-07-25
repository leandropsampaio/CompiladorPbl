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
    private boolean main = false;

    private boolean proximoToken = false;

    /**
     * Método que inicia a análise sintática.
     *
     * @param tokens lista de tokens extraídos da análise léxica
     * @param nomeArquivo
     */
    public void iniciar(ArrayList tokens, String nomeArquivo) throws IOException {
        saidaSemantico = new FileWriter("entrada\\saidaSintatico\\saida-" + nomeArquivo + ".txt");
        try {
            saidaSemantico.write("Análise Semântica iniciada para o arquivo " + nomeArquivo + "\n");

            System.out.println("Análise Semântica iniciada para o arquivo " + nomeArquivo);
            this.tokens = tokens;
            proximoToken();
            this.StringErrosSemanticos = "\n";
            Iterator iterador = this.tokens.listIterator();
            programa();

            if (errosSemanticos == 0 && !proximoToken && main == true) {

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

    private boolean proximoToken() {

        if (posicao + 1 < tokens.size()) {
            posicao++;
            tokenAnterior = tokenAtual;
            tokenAtual = tokens.get(posicao);
            proximoToken = true;
            return true;
        }
        Token tokenFinal = new Token("", "", tokenAnterior.getLinha() + 1);
        tokens.add(tokenFinal);
        tokenAtual = tokens.get(posicao + 1);
        proximoToken = false;
        return false;
    }

    private void tokenAnterior(int contador) {

        for (int i = 1; i <= contador; i++) {

            if (posicao - 1 < tokens.size()) {
                posicao--;
                tokenAtual = tokens.get(posicao);
                tokenAnterior = tokens.get(posicao - 1);
            }
        }
    }

    private boolean validarToken(String tipo) {

        if (tokenAtual.getTipo().equals(tipo) || tokenAtual.getNome().equals(tipo)) {
            proximoToken();
            return true;
        }

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

    public boolean declaracao() {

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
        } else if (proximoToken) {

            String mensagemErro = "- ERRO DE DECLARAÇÃO";
            this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";
            panicMode("declaracao");
            return true;
        }

        return false;
    }

    private boolean declaracaoDeFuncao() {

        if (validarToken("function")) {
            funcId();
            if (!validarToken("(")) {
                String mensagemErro = "- Faltou ( na declaração de fuction ";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

                panicMode("funcaoProcedimentoFim");
            }
            funcaoProcedimentoFim();
            return true;
        }

        return false;

    }

    private boolean declaracaoDeProcedimento() {

        if (validarToken("procedure")) {
            if (!validarToken("IDE")) {
                errosSemanticos++;
                String mensagemErro = "- Faltou o identificador na declaração do procedure";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

            }
            if (!validarToken("(")) {
                String mensagemErro = "- Faltou o ( na declaração do procedure";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

                panicMode("funcaoProcedimentoFim");
            }
            funcaoProcedimentoFim();
            return true;

        }

        return false;
    }

    private boolean declaracaoDeInicio() {

        if (validarToken("start")) {
            main = true;
            if (!validarToken("(")) {
                String mensagemErro = "- Faltou ( na declaracao de start";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";

                panicMode("bloco");
            }
            if (!validarToken(")")) {
                String mensagemErro = "- Faltou ) na declaracao de start";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

                panicMode("bloco");
            }
            bloco();
            return true;

        }

        return false;
    }

    private boolean declaracaoDeVar() {

        if (validarToken("var")) {

            if (!validarToken("{")) {
                String mensagemErro = "- Faltou a { na declaração de var";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

                panicMode("declaracaoDeVariavelCorpo");
            }
            declaracaoDeVariavelCorpo();
            if (!validarToken("}")) {
                String mensagemErro = "- Faltou a } na declaracao de var";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

                panicMode("declaracao");
            }
            return true;
        }

        return false;

    }

    private boolean declaracaoDeConst() {

        if (validarToken("const")) {

            if (!validarToken("{")) {
                String mensagemErro = "- Faltou a { na declaracao de const";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

                panicMode("declaracaoDeConstanteCorpo");
            }
            declaracaoDeConstanteCorpo();
            if (!validarToken("}")) {
                String mensagemErro = "- Faltou a } na declaracao de const";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

                panicMode("declaracao");
            }
            return true;
        }

        return false;
    }

    /*IMPLEMENTAR MODO PÂNICO!*/
    private boolean ifThen() {

        if (validarToken("if")) {
            if (!validarToken("(")) {
                String mensagemErro = "- Faltou o ( do If";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

                panicMode("expressao");
            }
            if (!expressao()) {
                errosSemanticos++;
                String mensagemErro = "- Faltou informar a expressão do IF";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

            }
            if (!validarToken(")")) {
                String mensagemErro = "- Faltou o ) do If";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

                panicMode("then");
            }
            if (!validarToken("then")) {
                String mensagemErro = "- Faltou o (then) da declaração do IF";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

                panicMode("bloco");
            }
            bloco();
            return true;
        }

        return false;
    }

    private void panicMode(String localSincronizar) {
        errosSemanticos++;
        switch (localSincronizar) {
            case "expressao":
                searchNextExpressao();
                break;
            case "then":
                searchNextThen();
                break;
            case "bloco":
                searchNextBloco();
                break;
            case "saida":
                searchNextSaida();
                break;
            case "entrada":
                searchNextEntrada();
                break;
            case "structCorpo":
                searchNextstructCorpo();
                break;
            case "listaDeInstrucoes":
                searchNextListaDeInstrucoes();
                break;
            case "funcaoProcedimentoFim":
                searchNextFuncaoProcedimentoFim();
                break;
            case "declaracaoDeVariavelCorpo":
                searchNextDeclaracaoDeVariavelCorpo();
                break;
            case "declaracao":
                searchNextDeclaracao();
                break;
            case "declaracaoDeConstanteCorpo":
                searchNextDeclaracaoDeConstanteCorpo();
                break;
            case "tipoAux":
                searchNextTipoAux();
                break;
            case "declaracaoDeStructCorpo":
                searchNextDeclaracaoDeStructCorpo();
                break;
            case "expressaoIdentificadoresStruct":
                searchNextExpressaoIdentificadoresStruct();
                break;
            case "tipoVetorDeclarando":
                searchNextTipoVetorDeclarando();
                break;
            case "parametroAux":
                searchNextParametroAux();
                break;
            case "parametros":
                searchNextParametros();
                break;
            case "simboloUnario":
                searchNextSimboloUnario();
                break;
            case "searchNextExpressaoIdentificadorVarAux":
                searchNextExpressaoIdentificadorVarAux();
                break;
            case "searchNextExpressaoIdentificadoresVarAux":
                searchNextExpressaoIdentificadoresVarAux();
                break;
        }
    }

    private boolean funcId() {
        metodoAtual = new Metodo();
        parametrosAtuais = new ArrayList();

        if (!tipo()) {

            errosSemanticos++;
            String mensagemErro = "- Faltou o tipo da função";
            this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";

        }
        metodoAtual.setNome(tokenAtual.getNome());
        if (!validarToken("IDE")) {
            errosSemanticos++;
            String mensagemErro = "- Faltou o nome da função";
            this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";

        }

        return false;
    }

    private boolean tipo() {

        if (tipobase()) {
            tipoAux();
            return true;
        }

        return false;
    }

    private boolean tipobase() {
        variavelAtual = new Variavel();
        if (validarToken("IDE")) {
            metodoAtual.setTipo(tokenAnterior.getNome());
            variavelAtual.setTipo(tokenAnterior.getNome());
            return true;
        } else if (escalar()) {
            metodoAtual.setTipo(tokenAnterior.getNome());
            variavelAtual.setTipo(tokenAnterior.getNome());
            return true;
        } else if (declaracaoDeStruct()) { // VERIFICAR
            return true;
        } else if (validarToken("struct")) {
            if (!validarToken("IDE")) {
                String mensagemErro = "- Faltou o identificador da struct";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";

                panicMode("tipoAux");
            }
            return true;
        }
        variavelAtual = new Variavel();
        return false;
    }

    private boolean escalar() {

        if (validarToken("int") || validarToken("float") || validarToken("bool") || validarToken("string")) {

            return true;
        }

        return false;
    }

    private boolean declaracaoDeStruct() {

        if (validarToken("struct")) {
            if (declaracaoDeStructAux()) {
                return true;
            }
            tokenAnterior(1);
        }

        return false;
    }

    private boolean declaracaoDeStructAux() {

        if (validarToken("IDE")) {
            if (tokenAtual.getTipo().equals("IDE")) {

                tokenAnterior(1);
                return false;
            }
            Extends();
            if (!validarToken("{")) {
                String mensagemErro = "- Faltou a { da struct";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";

                panicMode("structCorpo");
            }
            declaracaoDeStructCorpo();
            if (!validarToken("}")) {
                String mensagemErro = "- Faltou a } da struct ";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";

                panicMode("listaDeInstrucoes");
            }
            return true;
        } else if (Extends()) {
            if (!validarToken("{")) {
                String mensagemErro = "- Faltou a { da struct";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";

                panicMode("structCorpo");
            }
            declaracaoDeStructCorpo();
            if (!validarToken("}")) {
                String mensagemErro = "- Faltou a } da struct ";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";

                panicMode("listaDeInstrucoes");
            }
            return true;
        }

        return false;
    }

    private boolean Extends() {

        if (validarToken("extends")) {
            if (!validarToken("IDE")) {
                String mensagemErro = "- Faltou o identificador do extends";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";
                panicMode("declaracaoDeStructCorpo");
            }
        }

        return true;
    }

    private boolean declaracaoDeStructCorpo() {

        if (declaracaoDeStructLinha()) {
            declaracaoDeStructCorpoAux();
            return true;
        }

        return false;

    }

    private boolean declaracaoDeStructLinha() {

        if (tipo()) {
            expressaoIdentificadoresStruct();
            return true;
        }

        return false;
    }

    private boolean expressaoIdentificadoresStruct() {

        if (expressaoIdentificadorStruct()) {
            expressaoIdentificadoresStructAux();
            return true;
        }

        return false;
    }

    private boolean expressaoIdentificadorStruct() {

        if (validarToken("IDE")) {
            return true;
        }
        return false;
    }

    private boolean expressaoIdentificadoresStructAux() {

        if (!validarToken(";") && !tokenAtual.getNome().equals(",")) {
            String mensagemErro = "- Faltou o ;";
            this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";
            System.out.println("FALTOU O ; DO STRUCT" + tokenAtual.getLinha());

            panicMode("declaracaoDeStructCorpo");
            return true;
        } else if (!validarToken(",")) {

            if (tokenAtual.getTipo().equals("IDE")) {
                String mensagemErro = "- Faltou o identificador da struct";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";

                panicMode("expressaoIdentificadoresStruct");
            }
        }
        expressaoIdentificadoresStruct();

        return false;
    }

    private boolean tipoAux() {

        if (tipoVetorDeclarando()) {
            return true;
        }

        return false;
    }

    private boolean tipoVetorDeclarando() {

        if (tipoVetorDeclarado()) {
            tipoVetorDeclarandoAux();
            return true;
        }

        return false;
    }

    private boolean tipoVetorDeclarado() {

        if (validarToken("[")) {
            if (!validarToken("]")) {
                String mensagemErro = "- Faltou ] do vetor";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";

                panicMode("tipoVetorDeclarando");
            }
            return true;
        }

        return false;
    }

    private boolean tipoVetorDeclarandoAux() {

        tipoVetorDeclarando();

        return true;
    }

    private boolean declaracaoDeStructCorpoAux() {

        declaracaoDeStructCorpo();

        return true;
    }

    /**
     * ***********************************************************************
     */
    private boolean funcaoProcedimentoFim() {

        if (validarToken(")")) {

            if (bloco()) {

            }
            return true;
        } else if (parametros()) {

            if (!validarToken(")")) {
                String mensagemErro = "- Faltou o )";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

                panicMode("bloco");
            }
            if (bloco()) {

            }
            return true;
        } else {
            String mensagemErro = "- Faltou o )";
            this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

            panicMode("bloco");

            if (bloco()) {
                return true;
            }
        }

        return false;
    }

    private boolean parametros() {

        if (parametro()) {
            parametrosAux();
            return true;
        }

        return false;
    }

    private boolean bloco() {

        if (!validarToken("{")) {
            errosSemanticos++;
            String mensagemErro = "- Faltou { do bloco";
            this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

        }
        blocoAux();

        return false;
    }

    private boolean parametro() {

        if (tipo()) {
            parametroAtual.setTipo(tokenAnterior.getNome());

            if (!validarToken("IDE")) {
                String mensagemErro = "- Faltou o identificador do parâmetro";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";

                panicMode("parametrosAux");
                return false;
            }
            parametroAtual.setNome(tokenAnterior.getNome());
            return true;
        }

        return false;
    }

    private boolean parametrosAux() {

        if (!validarToken(",")) {
            if (showProx().getTipo().equals("IDE")) {
                String mensagemErro = "- Faltou virgula em parametros";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";

                panicMode("parametros");
            }
        }
        parametros();

        return true;
    }

    /*
        VERIFICAR!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    
     */
    private boolean blocoAux() {

        if (listaDeInstrucoes()) {

            if (!validarToken("}")) {
                errosSemanticos++;
                String mensagemErro = "- Faltou o fecha } do bloco";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";
                panicMode("*********************");
            }
        } else if (!validarToken("}")) {
            errosSemanticos++;
            String mensagemErro = "- Faltou o fecha } do bloco";
            this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";
        }

        return false;
    }

    private boolean listaDeInstrucoes() {

        if (instrucao()) {
            listaDeInstrucoesAux();
            return true;
        }

        return false;
    }

    private boolean instrucao() {

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

        return false;
    }

    private boolean listaDeInstrucoesAux() {
        listaDeInstrucoes();
        return true;
    }

    private boolean instrucaoNormal() {

        if (operacaoDeAtribuicao()) {
            if (!validarToken(";")) {
                errosSemanticos++;
                String mensagemErro = "- Faltou o ; da instrucao";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

            }
            return true;
        } else if (declaracaoDeStruct()) {
            if (!validarToken(";")) {
                errosSemanticos++;
                String mensagemErro = "- Faltou o ; da Struct";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

            }
            return true;
        } else if (Print()) {
            if (!validarToken(";")) {
                errosSemanticos++;
                String mensagemErro = "- Faltou o ; do Print";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

            }
            return true;
        } else if (scan()) {
            if (!validarToken(";")) {
                errosSemanticos++;
                String mensagemErro = "- Faltou o ; do Scan";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

            }
            return true;
        } else if (instrucaoDeRetorno()) {
            if (!validarToken(";")) {
                errosSemanticos++;
                String mensagemErro = "- Faltou ; do retorno";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

            }
            return true;
        }

        return false;
    }

    private boolean operacaoDeAtribuicao() {

        if (validarToken("IDE")) {

            if (validarToken("=")) {
                if (expressao()) {
                    return true;
                }
                tokenAnterior(2);
            }
            tokenAnterior(1);
        }
        if (Final()) {

            if (validarToken("=")) {
                if (expressao()) {
                    return true;
                }
                tokenAnterior(1);
            }
        }
        if (expressao()) {
            return true;
        }

        return false;
    }

    private boolean instrucaoDeRetorno() {

        if (validarToken("return")) {
            if (instrucaoDeRetornoAux()) {
                return true;
            }
        }

        return false;
    }

    private boolean Print() { // revisar...

        if (validarToken("print")) {
            if (!validarToken("(")) {
                errosSemanticos++;
                String mensagemErro = "- Faltou o ( do print ";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";
            }
            saida();
            outrasSaidas();
            if (!validarToken(")")) {
                errosSemanticos++;
                String mensagemErro = "- Faltou o ) do print ";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";
                System.out.println("FALTOU O ) DO PRINT");
                //panicMode("listaDeInstrucoes");
            }
            return true;
        }

        return false;
    }

    private boolean scan() {

        if (validarToken("scan")) {
            if (!validarToken("(")) {
                errosSemanticos++;
                String mensagemErro = "- Faltou o ( do scan";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";
                System.out.println("FALTOU O ( DO SCAN");
                //panicMode("entrada");
            }
            entrada();
            outrasEntradas();
            if (!validarToken(")")) {
                errosSemanticos++;
                String mensagemErro = "- Faltou o ) do scan ";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";
                System.out.println("FALTOU O ) DO SCAN");
                //panicMode("listaDeInstrucoes");
            }
            return true;
        }

        return false;
    }

    private boolean estruturaCondicional() {
        if (ifThen()) {
            estruturaCondicionalAux();
            return true;
        }
        return false;
    }

    private boolean While() {

        if (validarToken("while")) {
            if (!validarToken("(")) {
                String mensagemErro = "- Faltou ( do while ";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";

                panicMode("expressao");
            }
            if (!expressao()) {
                errosSemanticos++;
                String mensagemErro = "- Faltou a expressão do While";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";

            }
            if (!validarToken(")")) {
                String mensagemErro = "- Faltou ) do while ";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

                panicMode("bloco");
            }
            bloco();
            return true;
        }

        return false;
    }

    private boolean declaracaoDeTypedef() {

        if (validarToken("typedef")) {
            declaracaoDeTypedefAux();
            return true;
        }

        return false;
    }

    private boolean Final() {

        if (validarToken("IDE")) {

            if (acessando()) {
                return true;
            }
            tokenAnterior(1);
        }

        return false;
    }

    private boolean expressao() {

        if (opE()) {
            expressaoAux();
            return true;
        }

        return false;
    }

    private boolean instrucaoDeRetornoAux() {

        expressao();
        return true;
    }

    private boolean saida() {

        if (expressao()) {
            return true;
        }

        return false;
    }

    private boolean outrasSaidas() {

        if (validarToken(",")) {
            saida();
            if (outrasSaidas()) {
                return true;
            }
        }

        return true;
    }

    private boolean entrada() {
        Final();
        if (!validarToken("IDE")) {
            errosSemanticos++;
            String mensagemErro = "- Faltou o identificador";
            this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";
            System.out.println("- Faltou o identificador");
            return true;
        }

        return false;
    }

    private boolean outrasEntradas() {

        if (validarToken(",")) {
            entrada();
            if (outrasEntradas()) {
                return true;
            }
        }

        return true;
    }

    private boolean estruturaCondicionalAux() {

        if (validarToken("else")) {
            if (bloco()) {
                return true;
            }
        }

        return true;
    }

    private boolean declaracaoDeVariavelCorpo() {

        if (declaracaoDeVariavelLinha()) {
            if (declaracaoDeVariavelCorpoAux()) {
                return true;
            }
        }

        return false;
    }

    private boolean declaracaoDeVariavelLinha() {

        if (tipo()) {
            if (expressaoIdentificadoresVar()) {
                return true;
            }
        }

        return false;
    }

    private boolean declaracaoDeVariavelCorpoAux() {

        if (declaracaoDeVariavelCorpo()) {
            return true;
        }

        return true;
    }

    private boolean expressaoIdentificadoresVar() {

        if (expressaoIdentificadorVar()) {

        }
        if (expressaoIdentificadoresVarAux()) {
            return true;
        }

        return false;
    }

    private boolean expressaoIdentificadorVar() {

        if (validarToken("IDE")) {
            variavelAtual.setNome(tokenAnterior.getNome());
        } else {
            panicMode("expressaoIdentificadorVarAux");

            String mensagemErro = "- Faltou o Identificador";
            this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

        }
        if (expressaoIdentificadorVarAux()) {
            return true;
        }

        return false;
    }

    private boolean expressaoIdentificadoresVarAux() {

        if (tokenAtual.getTipo().equals("IDE")) {
            errosSemanticos++;
            String mensagemErro = "- Faltou a ,";
            this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

        }
        if (validarToken(",") || tokenAtual.getTipo().equals("IDE")) {
            if (expressaoIdentificadoresVar()) {
            }
            return true;
        }
        if (!validarToken(";")) {
            errosSemanticos++;
            String mensagemErro = "- Faltou o ;";
            this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

            return true;
        }

        return true;
    }

    private boolean expressaoIdentificadorVarAux() {

        if (validarToken("=")) {
            if (expressao()) {
                return true;
            }//if (showProx().getTipo().equals("IDE")) {
        }

        return true;
    }

    private boolean declaracaoDeConstanteCorpo() {

        if (declaracaoDeConstanteLinha()) {
            if (declaracaoDeConstanteCorpoAux()) {
                return true;
            }
        }

        return false;
    }

    private boolean declaracaoDeConstanteLinha() {

        if (tipo()) {
            if (expressaoIdentificadoresConst()) {
                return true;
            }
        }

        return false;
    }

    /**
     * comecei a partir daqui >>>>>>
     *
     */
    private boolean declaracaoDeConstanteCorpoAux() {

        if (declaracaoDeConstanteCorpo()) {
            return true;
        }

        return true;
    }

    private boolean expressaoIdentificadoresConst() {

        if (expressaoIdentificadorConst()) {
            expressaoIdentificadoresConstAux();
            return true;
        }

        return false;
    }

    /* VERIFICAR TOKEN ANTERIOR NO CONST*/
    private boolean expressaoIdentificadorConst() {

        if (validarToken("IDE")) {
            variavelAtual.setConstante(true);
            variavelAtual.setNome(tokenAnterior.getNome());
            if (!validarToken("=")) {
                errosSemanticos++;
                String mensagemErro = "- Faltou o = da expressão";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

            }
            if (!expressao()) {
                errosSemanticos++;
                String mensagemErro = "- Faltou informar o valor do atributo";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

            }
            return true;
            //tokenAnterior(1);
        }

        return false;
    }

    /*
    private boolean expressaoIdentificadoresConstAux() {

        if (tokenAtual.getTipo().equals("IDE")) {
            errosSemanticos++;
            String mensagemErro = "- Faltou a ,";
            this.StringErrosSintaticos = this.StringErrosSintaticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

        }
        if (validarToken(",") || tokenAtual.getTipo().equals("IDE")) {
            if (expressaoIdentificadoresConst()) {
                return true;
            }
        }
        if (!validarToken(";")) {
            errosSemanticos++;
            String mensagemErro = "- Faltou o ;";
            this.StringErrosSintaticos = this.StringErrosSintaticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

            return true;
        }

        return true;
    }
     */
    private boolean expressaoIdentificadoresConstAux() {
        System.out.println("EXPRESSAO IDENTIFICADORES CONST AUX");
        if (tokenAtual.getTipo().equals("IDE")) {
            errosSemanticos++;
            String mensagemErro = "- Faltou a ,";
            this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";
            System.out.println("Faltou a ,");
        }
        if (validarToken(",") || tokenAtual.getTipo().equals("IDE")) {
            if (expressaoIdentificadoresConst()) {
                return true;
            }
        }
        if (!validarToken(";")) {
            errosSemanticos++;
            String mensagemErro = "- Faltou o ;";
            this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";
            System.out.println("Faltou o ;");
            return true;
        }

        System.out.println("SAIDA EXPRESSAO IDENTIFICADOR CONST AUX");
        return true;
    }

    private boolean declaracaoDeTypedefAux() {

        if (!tipo()) {
            errosSemanticos++;
            String mensagemErro = "- Faltou o tipo do Typedef";
            this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

        }
        if (!validarToken("IDE")) {
            errosSemanticos++;
            String mensagemErro = "- Faltou o identificador do typedef";
            this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

            System.out.println("Faltou o identificador do typedef");
            //panicMode("listaDeInstrucoes");
        }
        if (!validarToken(";")) {
            errosSemanticos++;
            String mensagemErro = "- Faltou ; do typedef ";
            this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";
            System.out.println("FALTOU O ; NA DECLARACAO DE TYPEDEF ");
            //panicMode("listaDeInstrucoes");

        }
        if (!validarToken(";")) {
            errosSemanticos++;
            String mensagemErro = "- Faltou ; do typedef ";
            this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAnterior.getLinha() + "\n";

            //panicMode("listaDeInstrucoes");
        }

        return false;
    }

    private boolean acessando() {

        if (acesso()) {
            acessandoAux();
            return true;
        }

        return false;
    }

    private boolean acesso() {

        if (validarToken(".")) {
            if (!validarToken("IDE")) {
                String mensagemErro = "- Faltou identificador no acesso ";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";

                panicMode("listaDeInstrucoes");
            }
            return true;
        } else if (validarToken("[")) {
            expressao();
            if (!validarToken("]")) {
                String mensagemErro = "- Faltou ] ";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";

                panicMode("listaDeInstrucoes");
            }
            return true;
        }

        return false;
    }

    private boolean acessandoAux() {

        acessando();

        return true;
    }

    private boolean opE() {

        if (opRelacional()) {
            opEAux();
            return true;
        }

        return false;
    }

    private boolean expressaoAux() {

        if (validarToken("||")) {
            if (expressao()) {
                return true;
            } else {
                return false;
            }
        }

        return true;
    }

    private boolean opRelacional() {

        if (valorRelacional()) {
            opRelacionalAux();
            return true;
        }

        return false;
    }

    private boolean opEAux() {

        if (validarToken("&&")) {
            opE();
            return true;
        }

        return true;
    }

    private boolean valorRelacional() {

        if (opMult()) {
            valorRelacionalAux();
            return true;

        }

        return false;
    }

    private boolean opRelacionalAux() {

        if (escalarRelacional()) {
            opRelacional();
            return true;
        }

        return true;
    }

    private boolean opMult() {

        if (opUnary()) {
            opMultAux();
            return true;
        }

        return false;
    }

    private boolean valorRelacionalAux() {

        if (validarToken("+")) {
            opMult();
            valorRelacionalAux();
            return true;

        } else if (validarToken("-")) {
            opMult();
            valorRelacionalAux();
            return true;
        }

        return true;
    }

    private boolean escalarRelacional() {

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

        return false;
    }

    private boolean opUnary() {

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

        return false;
    }

    private boolean opMultAux() {

        if (validarToken("*")) {
            opUnary();
            opMultAux();
            return true;

        } else if (validarToken("/")) {
            opUnary();
            opMultAux();
            return true;
        }

        return true;
    }

    private boolean simboloUnario() {

        if (validarToken("++")) {
            return true;
        } else if (validarToken("--")) {
            return true;
        }

        return true;
    }

    private boolean valor() {

        if (validarToken("(")) {
            expressao();
            if (!validarToken(")")) {
                String mensagemErro = "- Faltou ) ";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";
                panicMode("simboloUnario");
            }
            return true;
        } else if (validarToken("NRO")) {
            return true;
        } else if (validarToken("CAD")) {
            return true;
        } else if (validarToken("true")) {
            return true;
        } else if (validarToken("false")) {
            return true;
        } else if (validarToken("IDE")) {

            if (valorAux1()) {
                return true;
            }
            tokenAnterior(1);
        }

        return false;
    }

    private boolean valorAux1() {

        if (validarToken("(")) {
            valorAux2();
        }

        return true;
    }

    private boolean valorAux2() {

        if (parametrosFuncao()) {
            if (!validarToken(")")) {
                String mensagemErro = "- Faltou ) ";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";

                panicMode("simboloUnario");
            }
            return true;
        } else if (!validarToken(")")) {
            panicMode("simboloUnario");
        }

        return false;
    }

    private boolean parametrosFuncao() {

        if (expressao()) {
            parametrosFuncaoAux();
            return true;
        }

        return false;
    }

    private boolean parametrosFuncaoAux() {

        if (!validarToken(",")) {
            if (!tokenAtual.getNome().equals(")")) { ///////////////////////***//**/*/**/*/*/*/*/*//*/**/*/*/*/*/*/*/*/
                String mensagemErro = "- Faltou , ";
                this.StringErrosSemanticos = this.StringErrosSemanticos + mensagemErro + " na linha:" + tokenAtual.getLinha() + "\n";

                panicMode("parametros");
            }
            return true;
        }
        parametrosFuncao();

        return true;
    }

    private boolean firstForaDeBloco() {
        return tokenAtual.getNome().equals("const") || tokenAtual.getNome().equals("var") || tokenAtual.getNome().equals("struct")
                || tokenAtual.getNome().equals("typedef") || tokenAtual.getNome().equals("procedure") || tokenAtual.getNome().equals("function")
                || tokenAtual.getNome().equals("return") || tokenAtual.getNome().equals("start")
                || tokenAtual.getNome().equals("then") || tokenAtual.getNome().equals("else") || tokenAtual.getNome().equals("while")
                || tokenAtual.getNome().equals("scan") || tokenAtual.getNome().equals("print") || tokenAtual.getNome().equals("int")
                || tokenAtual.getNome().equals("float") || tokenAtual.getNome().equals("bool") || tokenAtual.getNome().equals("string")
                || tokenAtual.getNome().equals("true") || tokenAtual.getNome().equals("false") || tokenAtual.getNome().equals("extends");
    }

    private boolean firstDentroDeBloco() {
        return tokenAtual.getNome().equals("int") || tokenAtual.getNome().equals("float") || tokenAtual.getTipo().equals("IDE")
                || tokenAtual.getNome().equals("string") || tokenAtual.getNome().equals("bool");

    }

    /**
     * Procura o proximo token de sincronização
     */
    private void searchNextExpressao() {
        if (tokenAtual.getNome().equals("--") || tokenAtual.getNome().equals("!") || tokenAtual.getNome().equals("(")
                || tokenAtual.getNome().equals("++") || tokenAtual.getTipo().equals("CAD")
                || tokenAtual.getTipo().equals("DIG") || tokenAtual.getNome().equals("false")
                || tokenAtual.getTipo().equals("IDE") || tokenAtual.getNome().equals("true")) {

        } else if (firstForaDeBloco()) {

        } else if (firstDentroDeBloco()) {

        } else if (proximoToken()) {
            searchNextExpressao();
        }
    }

    private void searchNextThen() {
        if (tokenAtual.getNome().equals("then")) {
        } else if (firstForaDeBloco()) {

        } else if (firstDentroDeBloco()) {

        } else if (proximoToken()) {
            searchNextThen();
        }
    }

    private void searchNextBloco() {

        if (tokenAtual.getNome().equals("{")) {

        } else if (firstForaDeBloco()) {

        } else if (firstDentroDeBloco()) {

        } else if (proximoToken()) {
            searchNextBloco();
        }
    }

    private void searchNextListaDeInstrucoes() {
        if (tokenAtual.getNome().equals("--") || tokenAtual.getNome().equals("!")
                || tokenAtual.getNome().equals("(") || tokenAtual.getNome().equals("++")
                || tokenAtual.getTipo().equals("CAD") || tokenAtual.getTipo().equals("DIG")
                || tokenAtual.getNome().equals("false") || tokenAtual.getTipo().equals("IDE")
                || tokenAtual.getNome().equals("print") || tokenAtual.getNome().equals("return")
                || tokenAtual.getNome().equals("scan") || tokenAtual.getNome().equals("struct")
                || tokenAtual.getNome().equals("true") || tokenAtual.getNome().equals("typedef")
                || tokenAtual.getNome().equals("var") || tokenAtual.getNome().equals("while")
                || tokenAtual.getNome().equals(")")) {

        } else if (firstDentroDeBloco()) {

        } else if (proximoToken()) {
            searchNextListaDeInstrucoes();
        }
    }

    private void searchNextstructCorpo() {
        if (tokenAtual.getNome().equals("bool") || tokenAtual.getNome().equals("float")
                || tokenAtual.getTipo().equals("IDE") || tokenAtual.getNome().equals("int")
                || tokenAtual.getNome().equals("string") || tokenAtual.getNome().equals("struct")) {
        } else if (firstForaDeBloco()) {

        } else if (firstDentroDeBloco()) {

        } else if (proximoToken()) {
            searchNextstructCorpo();
        }
    }

    private void searchNextSaida() {
        if (tokenAtual.getNome().equals("--") || tokenAtual.getNome().equals("!")
                || tokenAtual.getNome().equals("(") || tokenAtual.getNome().equals("++")
                || tokenAtual.getTipo().equals("CAD") || tokenAtual.getTipo().equals("DIG")
                || tokenAtual.getNome().equals("false") || tokenAtual.getTipo().equals("IDE")
                || tokenAtual.getNome().equals("true")) {
        } else if (firstDentroDeBloco()) {

        } else if (proximoToken()) {
            searchNextSaida();
        }
    }

    private void searchNextEntrada() {
        if (tokenAtual.getTipo().equals("IDE")) {
        } else if (firstDentroDeBloco()) {
        } else if (proximoToken()) {
            searchNextEntrada();
        }
    }

    private void searchNextDeclaracaoDeStructCorpo() {
        if (tokenAtual.getNome().equals("bool") || tokenAtual.getNome().equals("float")
                || tokenAtual.getTipo().equals("IDE") || tokenAtual.getNome().equals("int")
                || tokenAtual.getNome().equals("string") || tokenAtual.getNome().equals("struct")) {
        } else if (firstDentroDeBloco()) {
        } else if (firstForaDeBloco()) {
        } else if (proximoToken()) {
            searchNextDeclaracaoDeStructCorpo();
        }
    }

    private void searchNextFuncaoProcedimentoFim() {
        if (tokenAtual.getNome().equals(")") || tokenAtual.getNome().equals("bool")
                || tokenAtual.getNome().equals("float") || tokenAtual.getTipo().equals("IDE")
                || tokenAtual.getNome().equals("int") || tokenAtual.getNome().equals("string")
                || tokenAtual.getNome().equals("struct")) {
        } else if (firstForaDeBloco()) {

        } else if (proximoToken()) {
            searchNextFuncaoProcedimentoFim();
        }
    }

    private void searchNextDeclaracaoDeVariavelCorpo() {
        if (tokenAtual.getNome().equals("bool") || tokenAtual.getNome().equals("float")
                || tokenAtual.getTipo().equals("IDE") || tokenAtual.getNome().equals("int")
                || tokenAtual.getNome().equals("string") || tokenAtual.getNome().equals("struct")) {
        } else if (firstForaDeBloco()) {

        } else if (firstDentroDeBloco()) {

        } else if (proximoToken()) {
            searchNextDeclaracaoDeVariavelCorpo();
        }
    }

    private void searchNextDeclaracao() {
        if (tokenAtual.getNome().equals("const") || tokenAtual.getNome().equals("function")
                || tokenAtual.getNome().equals("procedure") || tokenAtual.getNome().equals("start")
                || tokenAtual.getNome().equals("typedef") || tokenAtual.getNome().equals("var")) {

        } else if (proximoToken()) {
            searchNextDeclaracao();
        }
    }

    private void searchNextDeclaracaoDeConstanteCorpo() {
        if (tokenAtual.getNome().equals("bool") || tokenAtual.getNome().equals("float")
                || tokenAtual.getTipo().equals("IDE") || tokenAtual.getNome().equals("int")
                || tokenAtual.getNome().equals("string") || tokenAtual.getNome().equals("struct")) {
        } else if (firstForaDeBloco()) {

        } else if (firstDentroDeBloco()) {

        } else if (proximoToken()) {
            searchNextDeclaracaoDeConstanteCorpo();
        }
    }

    private void searchNextTipoAux() {
        if (tokenAtual.getNome().equals("[")) {
        } else if (firstDentroDeBloco()) {

        } else if (proximoToken()) {
            searchNextTipoAux();
        }
    }

    private void searchNextExpressaoIdentificadoresStruct() {
        if (tokenAtual.getTipo().equals("IDE")) {
        } else if (firstForaDeBloco()) {

        } else if (firstDentroDeBloco()) {

        } else if (proximoToken()) {
            searchNextExpressaoIdentificadoresStruct();
        }
    }

    private void searchNextTipoVetorDeclarando() {
        if (tokenAtual.getNome().equals("{")) {
        } else if (firstDentroDeBloco()) {

        } else if (proximoToken()) {
            searchNextTipoVetorDeclarando();
        }
    }

    private void searchNextParametroAux() {
        if (tokenAtual.getNome().equals(",")) {
        } else if (firstForaDeBloco()) {

        } else if (firstDentroDeBloco()) {

        } else if (proximoToken()) {
            searchNextParametroAux();
        }
    }

    private void searchNextParametros() {
        if (tokenAtual.getNome().equals("bool") || tokenAtual.getNome().equals("float")
                || tokenAtual.getTipo().equals("IDE") || tokenAtual.getNome().equals("int")
                || tokenAtual.getNome().equals("string") || tokenAtual.getNome().equals("struct")) {
        } else if (firstForaDeBloco()) {

        } else if (firstDentroDeBloco()) {

        } else if (proximoToken()) {
            searchNextParametros();
        }
    }

    private void searchNextSimboloUnario() {
        if (tokenAtual.getNome().equals("--") || tokenAtual.getNome().equals("++")) {
        } else if (firstDentroDeBloco()) {

        } else if (proximoToken()) {
            searchNextSimboloUnario();
        }
    }

    private void searchNextExpressaoIdentificadorVarAux() {
        if (tokenAtual.getNome().equals("=") || tokenAtual.getNome().equals(",") || tokenAtual.getNome().equals(";")) {
        } else if (firstForaDeBloco()) {

        } else if (firstDentroDeBloco()) {

        } else if (proximoToken()) {
            searchNextExpressaoIdentificadorVarAux();
        }
    }

    private void searchNextExpressaoIdentificadoresVarAux() {
        if (tokenAtual.getNome().equals(",") || tokenAtual.getNome().equals(";")) {
        } else if (firstForaDeBloco()) {

        } else if (firstDentroDeBloco()) {

        } else if (proximoToken()) {
            searchNextExpressaoIdentificadoresVarAux();
        }
    }

    private void addVariavel() {
        if (classeAtual == null) {
            if (!global.addVariavel(variavelAtual)) {
                //erro ao add variavel
                ///////////////////////         tokenAnteriorAnterior ********************************************
                ///////////////////////         tokenAnteriorAnterior ********************************************
                salvarMensagemArquivo("Variável já existente com esse nome. Linha: " + tokenAnterior.getLinha());
            }
        } else if (metodoAtual == null) {
            if (!classeAtual.addVariavel(variavelAtual)) {
                //erro ao add variavel
                salvarMensagemArquivo("Variável já existente com esse nome. Linha: " + tokenAnterior.getLinha());
            }
        } else if (!metodoAtual.addVariavel(variavelAtual)) {
            //erro ao add variavel
            salvarMensagemArquivo("Variável já existente com esse nome. Linha: " + tokenAnterior.getLinha());
        }
    }

    private void addParametro() {
        if (!parametrosAtuais.contains(parametroAtual)) {
            parametrosAtuais.add(parametroAtual);
        } else {
            //erro parametro já existe com esse nome
            salvarMensagemArquivo("Parâmetro já existente com esse nome. Linha: " + +tokenAtual.getLinha());
        }
    }

    private void addMetodo() {
        if (classeAtual == null) {
            //tentando declarar metodo fora da classe erro
            salvarMensagemArquivo("Método declarado fora de uma classe. Linha: " + tokenAtual.getLinha());
        } else {
            classeAtual.addMetodo(metodoAtual);
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
        } else {
            int num = Integer.parseInt(numero);
            if (num < 1) {
                System.out.println("Erro! Tamanho do vetor menor 1");
                salvarMensagemArquivo("Erro! Tamanho do vetor menor 1. Linha: " + tokenAnterior.getLinha());
            }
        }
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