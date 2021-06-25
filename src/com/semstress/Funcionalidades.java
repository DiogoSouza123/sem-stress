/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.semstress;

import java.util.List;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JToggleButton;
import com.enums.NomeIconeEnum;

/**
 *
 * @author DiogoSouza
 */
public class Funcionalidades {
    
    private IconesJogo iconesJogo = new IconesJogo();
    
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
    
    public String verificarMatchVerticalColuna(List<JToggleButton> jToggleButtons){

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
            
            return "100";
        }else if(verificarQuatroElementos(jToggleButtons.get(3), jToggleButtons.get(4), jToggleButtons.get(5), jToggleButtons.get(6))){
            //verificar elementos 4,5,6,7
            trocarIcones(jToggleButtons.get(6), jToggleButtons.get(2));
            trocarIcones(jToggleButtons.get(5), jToggleButtons.get(1));
            trocarIcones(jToggleButtons.get(4), jToggleButtons.get(0));
            
            atualizarIcone(jToggleButtons.get(0));
            atualizarIcone(jToggleButtons.get(1));
            atualizarIcone(jToggleButtons.get(2));
            return "100";
            
        }else if(verificarQuatroElementos(jToggleButtons.get(2), jToggleButtons.get(3), jToggleButtons.get(4), jToggleButtons.get(5))){
            //verificar elementos 3, 4, 5, 6
            trocarIcones(jToggleButtons.get(5), jToggleButtons.get(1));
            trocarIcones(jToggleButtons.get(4), jToggleButtons.get(0));
            
            atualizarIcone(jToggleButtons.get(0));
            atualizarIcone(jToggleButtons.get(1));
            atualizarIcone(jToggleButtons.get(2));
            atualizarIcone(jToggleButtons.get(3));
            return "100";
            
        }else if(verificarQuatroElementos(jToggleButtons.get(1), jToggleButtons.get(2), jToggleButtons.get(3), jToggleButtons.get(4))){
            //verificar elementos 2, 3, 4, 5
            trocarIcones(jToggleButtons.get(4), jToggleButtons.get(0));
            
            atualizarIcone(jToggleButtons.get(0));
            atualizarIcone(jToggleButtons.get(1));
            atualizarIcone(jToggleButtons.get(2));
            atualizarIcone(jToggleButtons.get(3));
            return "100";
            
        }else if(verificarQuatroElementos(jToggleButtons.get(0), jToggleButtons.get(1), jToggleButtons.get(2), jToggleButtons.get(3))){
            //verificar elementos 1, 2, 3, 4
            
            atualizarIcone(jToggleButtons.get(0));
            atualizarIcone(jToggleButtons.get(1));
            atualizarIcone(jToggleButtons.get(2));
            atualizarIcone(jToggleButtons.get(3));
            return "100";
        }
        
        return "0";
    }
    
    public boolean verificarQuatroElementos(JToggleButton jToggleButton1, JToggleButton jToggleButton2, JToggleButton jToggleButton3,JToggleButton jToggleButton4){
        
        return jToggleButton1.getText().equals(jToggleButton2.getText())
                && jToggleButton2.getText().equals(jToggleButton3.getText())
                && jToggleButton3.getText().equals(jToggleButton4.getText());
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
    
}
