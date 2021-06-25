/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.semstress;

import java.awt.Image;
import javax.swing.ImageIcon;
import com.enums.NomeIconeEnum;

/**
 *
 * @author DiogoSouza
 */
public class IconesJogo {
    
    private ImageIcon coffeWhite = new ImageIcon(getClass().getResource("images/coffee-white.png"));
    private ImageIcon coffeBeans = new ImageIcon(getClass().getResource("images/coffee-beans.png"));
    private ImageIcon coffeBrown = new ImageIcon(getClass().getResource("images/coffee-brown.png"));
    private ImageIcon coffeYellow = new ImageIcon(getClass().getResource("images/coffee-yellow.png"));
    private ImageIcon coffeRed = new ImageIcon(getClass().getResource("images/coffee-red.png"));
    
    public ImageIcon retornarIcone(NomeIconeEnum nomeIconeEnum){
        
        switch (nomeIconeEnum) {
            case COFFEE_WHITE:
            {
                Image coffeImg = coffeWhite.getImage();
                Image coffeImgNew = coffeImg.getScaledInstance(20, 20,  java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
                return coffeWhite = new ImageIcon(coffeImgNew);
            }
            case COFFEE_BEANS:
            {
                Image coffeImg = coffeBeans.getImage();
                Image coffeImgNew = coffeImg.getScaledInstance(20, 20,  java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
                return coffeBeans = new ImageIcon(coffeImgNew);
            }
            case COFFEE_BROWN:
            {
                Image coffeImg = coffeBrown.getImage();
                Image coffeImgNew = coffeImg.getScaledInstance(20, 20,  java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
                return coffeBrown = new ImageIcon(coffeImgNew);
            }
            case COFFEE_RED:
            {
                Image coffeImg = coffeRed.getImage();
                Image coffeImgNew = coffeImg.getScaledInstance(20, 20,  java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
                return coffeRed = new ImageIcon(coffeImgNew);
            }
            case COFFEE_YELLOW:
            {
                Image coffeImg = coffeYellow.getImage();
                Image coffeImgNew = coffeImg.getScaledInstance(20, 20,  java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
                return coffeYellow = new ImageIcon(coffeImgNew);
            }
            default:
                break;
        }
        return null;
        
    }
    
}
