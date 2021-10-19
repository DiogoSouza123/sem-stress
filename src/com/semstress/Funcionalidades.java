/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.semstress;

import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JToggleButton;
import com.enums.NomeIconeEnum;

/**
 *
 * @author DiogoSouza
 */
public class Funcionalidades {
    
    private IconesJogo iconesJogo = new IconesJogo();
    private static int MATCH4PONTOS = 1000;
    private static int MATCH3PONTOS = 500;
    
    public String gerarValorRandomico(){
        double resultado = Math.random()*4;
        return String.format("%.0f", resultado);
    }
    
    public void trocarValores(List<JCheckBox>checkBoxSelected){
        JCheckBox checkBox1 = checkBoxSelected.get(0);
        JCheckBox checkBox2 = checkBoxSelected.get(1);
        JCheckBox checkBoxTemp = new JCheckBox();
        
        checkBoxTemp.setText(checkBox1.getText());
        checkBox1.setText(checkBox2.getText());
        checkBox2.setText(checkBoxTemp.getText());
    }
    
    public int verificarMatchVerticalColuna(List<JToggleButton> jToggleButtons){

        //Verificacao match de 4 elementos
        if(verificarQuatroElementos(jToggleButtons.get(4), jToggleButtons.get(5), jToggleButtons.get(6), jToggleButtons.get(7))){
            //verificar elementos 5, 6, 7, 8 dão match
            trocarIcones(jToggleButtons.get(7), jToggleButtons.get(3));
            trocarIcones(jToggleButtons.get(6), jToggleButtons.get(2));
            trocarIcones(jToggleButtons.get(5), jToggleButtons.get(1));
            trocarIcones(jToggleButtons.get(4), jToggleButtons.get(0));
            
            atualizarIcone(jToggleButtons.get(0));
            atualizarIcone(jToggleButtons.get(1));
            atualizarIcone(jToggleButtons.get(2));
            atualizarIcone(jToggleButtons.get(3));
            
            return MATCH4PONTOS;
        }else if(verificarQuatroElementos(jToggleButtons.get(3), jToggleButtons.get(4), jToggleButtons.get(5), jToggleButtons.get(6))){
            //verificar elementos 4,5,6,7
            trocarIcones(jToggleButtons.get(6), jToggleButtons.get(2));
            trocarIcones(jToggleButtons.get(5), jToggleButtons.get(1));
            trocarIcones(jToggleButtons.get(4), jToggleButtons.get(0));
            
            atualizarIcone(jToggleButtons.get(0));
            atualizarIcone(jToggleButtons.get(1));
            atualizarIcone(jToggleButtons.get(2));
            return MATCH4PONTOS;
            
        }else if(verificarQuatroElementos(jToggleButtons.get(2), jToggleButtons.get(3), jToggleButtons.get(4), jToggleButtons.get(5))){
            //verificar elementos 3, 4, 5, 6
            trocarIcones(jToggleButtons.get(5), jToggleButtons.get(1));
            trocarIcones(jToggleButtons.get(4), jToggleButtons.get(0));
            
            atualizarIcone(jToggleButtons.get(0));
            atualizarIcone(jToggleButtons.get(1));
            atualizarIcone(jToggleButtons.get(2));
            atualizarIcone(jToggleButtons.get(3));
            return MATCH4PONTOS;
            
        }else if(verificarQuatroElementos(jToggleButtons.get(1), jToggleButtons.get(2), jToggleButtons.get(3), jToggleButtons.get(4))){
            //verificar elementos 2, 3, 4, 5
            trocarIcones(jToggleButtons.get(4), jToggleButtons.get(0));
            
            atualizarIcone(jToggleButtons.get(0));
            atualizarIcone(jToggleButtons.get(1));
            atualizarIcone(jToggleButtons.get(2));
            atualizarIcone(jToggleButtons.get(3));
            return MATCH4PONTOS;
            
        }else if(verificarQuatroElementos(jToggleButtons.get(0), jToggleButtons.get(1), jToggleButtons.get(2), jToggleButtons.get(3))){
            //verificar elementos 1, 2, 3, 4
            
            atualizarIcone(jToggleButtons.get(0));
            atualizarIcone(jToggleButtons.get(1));
            atualizarIcone(jToggleButtons.get(2));
            atualizarIcone(jToggleButtons.get(3));
            return MATCH4PONTOS;
        }
        
        //fim verificacao 4 elementos e inicio verificacao 3 elementos
        else if(verificaTresElementos(jToggleButtons.get(7), jToggleButtons.get(6), jToggleButtons.get(5))){
            //verificar elementos 5 6 7 dão match
            trocarIcones(jToggleButtons.get(7), jToggleButtons.get(4));
            trocarIcones(jToggleButtons.get(6), jToggleButtons.get(3));
            trocarIcones(jToggleButtons.get(5), jToggleButtons.get(2));
            trocarIcones(jToggleButtons.get(4), jToggleButtons.get(1));
            trocarIcones(jToggleButtons.get(3), jToggleButtons.get(0));
            
            atualizarIcone(jToggleButtons.get(0));
            atualizarIcone(jToggleButtons.get(1));
            atualizarIcone(jToggleButtons.get(2));
            return MATCH3PONTOS;
        }else if(verificaTresElementos(jToggleButtons.get(6), jToggleButtons.get(5), jToggleButtons.get(4))){
            //TODO criar funcao verifica 4 5 6
            trocarIcones(jToggleButtons.get(6), jToggleButtons.get(3));
            trocarIcones(jToggleButtons.get(5), jToggleButtons.get(2));
            trocarIcones(jToggleButtons.get(4), jToggleButtons.get(1));
            trocarIcones(jToggleButtons.get(3), jToggleButtons.get(0));
            
            atualizarIcone(jToggleButtons.get(0));
            atualizarIcone(jToggleButtons.get(1));
            atualizarIcone(jToggleButtons.get(2));
            return MATCH3PONTOS;
        }else if(verificaTresElementos(jToggleButtons.get(5), jToggleButtons.get(4), jToggleButtons.get(3))){
            //TODO criar funcao verifica 3 4 5
            trocarIcones(jToggleButtons.get(5), jToggleButtons.get(2));
            trocarIcones(jToggleButtons.get(4), jToggleButtons.get(1));
            trocarIcones(jToggleButtons.get(3), jToggleButtons.get(0));
            
            atualizarIcone(jToggleButtons.get(0));
            atualizarIcone(jToggleButtons.get(1));
            atualizarIcone(jToggleButtons.get(2));
            return MATCH3PONTOS;
        }else if(verificaTresElementos(jToggleButtons.get(4), jToggleButtons.get(3), jToggleButtons.get(2))){
            //TODO criar funcao verifica 2 3 4
            trocarIcones(jToggleButtons.get(4), jToggleButtons.get(1));
            trocarIcones(jToggleButtons.get(3), jToggleButtons.get(0));
            
            atualizarIcone(jToggleButtons.get(0));
            atualizarIcone(jToggleButtons.get(1));
            atualizarIcone(jToggleButtons.get(2));
            return MATCH3PONTOS;
        }else if(verificaTresElementos(jToggleButtons.get(3), jToggleButtons.get(2), jToggleButtons.get(1))){
            //TODO criar funcao verifica 1 2 3
            trocarIcones(jToggleButtons.get(3), jToggleButtons.get(0));
            
            atualizarIcone(jToggleButtons.get(0));
            atualizarIcone(jToggleButtons.get(1));
            atualizarIcone(jToggleButtons.get(2));
            return MATCH3PONTOS;
        }else if(verificaTresElementos(jToggleButtons.get(2), jToggleButtons.get(1), jToggleButtons.get(0))){
            //TODO criar funcao verifica 0 1 2
            atualizarIcone(jToggleButtons.get(0));
            atualizarIcone(jToggleButtons.get(1));
            atualizarIcone(jToggleButtons.get(2));
            return MATCH3PONTOS;
        }
                
        return 0;
    }
    
    public boolean verificarQuatroElementos(JToggleButton jToggleButton1, JToggleButton jToggleButton2, JToggleButton jToggleButton3,JToggleButton jToggleButton4){
        
        return jToggleButton1.getText().equals(jToggleButton2.getText())
                && jToggleButton2.getText().equals(jToggleButton3.getText())
                && jToggleButton3.getText().equals(jToggleButton4.getText());
    }
    
    public boolean verificaTresElementos(JToggleButton jToggleButton1, JToggleButton jToggleButton2, JToggleButton jToggleButton3){
        return jToggleButton1.getText().equals(jToggleButton2.getText())
                && jToggleButton2.getText().equals(jToggleButton3.getText());
    }
    
    public void trocarIcones(JToggleButton jToggleButtonPai, JToggleButton jToggleButtonFilho){
            jToggleButtonPai.setText(jToggleButtonFilho.getText());
            jToggleButtonPai.setIcon(jToggleButtonFilho.getIcon());
            
    }
    
    public void atualizarIcone(JToggleButton jToggleButton){
        jToggleButton.setText(gerarValorRandomico());
        atualizarCampoSelecionado(jToggleButton);
    }
    
    public void atualizarCampoSelecionado(JToggleButton jToggleButton){
        
        switch (jToggleButton.getText()) {
            case "0":
                jToggleButton.setIcon(iconesJogo.retornarIcone(NomeIconeEnum.COFFEE_BEANS));
                break;
            case "1":
                jToggleButton.setIcon(iconesJogo.retornarIcone(NomeIconeEnum.COFFEE_BROWN));
                break;
            case "2":
                jToggleButton.setIcon(iconesJogo.retornarIcone(NomeIconeEnum.COFFEE_WHITE));
                break;
            case "3":
                jToggleButton.setIcon(iconesJogo.retornarIcone(NomeIconeEnum.COFFEE_YELLOW));
                break;
            case "4":
                jToggleButton.setIcon(iconesJogo.retornarIcone(NomeIconeEnum.COFFEE_RED));
                break;
            default:
                break;
        }

    }
    
    public int verificarMatchHorizontalColuna (JToggleButton[][] botoes){
        
        int pontos = 0;
        
        //1 - Verificar combinações de 4 na horizontal, de cima para baixo, da esquerda para direita
        for(int i = 0 ; i < 8 ; i++){
            for(int j = 0 ; j < 3 ;j++){
                
                if(verificarQuatroElementos(botoes[i][j], botoes[i][j+1], botoes[i][j+2], botoes[i][j+3])){
                    botoes[i][j].setText("5");
                    botoes[i][j].setIcon(iconesJogo.retornarIcone(NomeIconeEnum.FIRE));
                    
                    botoes[i][j+1].setText("5");
                    botoes[i][j+1].setIcon(iconesJogo.retornarIcone(NomeIconeEnum.FIRE));
                    
                    botoes[i][j+2].setText("5");
                    botoes[i][j+2].setIcon(iconesJogo.retornarIcone(NomeIconeEnum.FIRE));
                    
                    botoes[i][j+3].setText("5");
                    botoes[i][j+3].setIcon(iconesJogo.retornarIcone(NomeIconeEnum.FIRE));
                    
                    pontos += 1000;
                    
                    removeEspacosQueimados(botoes);
                    
                }
                    
            }
        }
        
        //2 - Verificar combinações de 3 na horizontal, de cima para baixo, da esquerda para direita
        for(int i = 0 ; i < 8 ; i++){
            for(int j = 0 ; j < 4 ;j++){
                
                if(verificaTresElementos(botoes[i][j], botoes[i][j+1], botoes[i][j+2])){
                    botoes[i][j].setText("5");
                    botoes[i][j].setIcon(iconesJogo.retornarIcone(NomeIconeEnum.FIRE));
                    
                    botoes[i][j+1].setText("5");
                    botoes[i][j+1].setIcon(iconesJogo.retornarIcone(NomeIconeEnum.FIRE));
                    
                    botoes[i][j+2].setText("5");
                    botoes[i][j+2].setIcon(iconesJogo.retornarIcone(NomeIconeEnum.FIRE));
                    
                    pontos += 500;
                }
                    
            }
        }
        
        return pontos;
        
    }
    
    public void removeEspacosQueimados(JToggleButton[][] botoes){
        
        for(int i = 0 ; i <= 7 ; i++){
            for(int j = 7 ; j >= 0 ; j--){
                System.out.println("J="+j+"  I="+i);
            }
        }
        
    }
}
