/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.semstress;

import java.awt.Image;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import com.enums.NomeIconeEnum;

/**
 *
 * @author DiogoSouza
 */
public class IconesJogo {
    private static final int TAMANHO_PECA = 40;
    private final ConfiguracaoJogo configuracao = ConfiguracaoJogo.get();
    
    private ImageIcon coffeWhite = carregarIconePeca("coffee-white");
    private ImageIcon coffeBeans = carregarIconePeca("coffee-beans");
    private ImageIcon coffeBrown = carregarIconePeca("coffee-brown");
    private ImageIcon coffeYellow = carregarIconePeca("coffee-yellow");
    private ImageIcon coffeRed = carregarIconePeca("coffee-red");
    private ImageIcon fire = carregarIconePeca("fire");
    private ImageIcon explosao = carregarIconeExplosao();
    
    public ImageIcon retornarIcone(NomeIconeEnum nomeIconeEnum){
        
        switch (nomeIconeEnum) {
            case COFFEE_WHITE:
            {
                return coffeWhite;
            }
            case COFFEE_BEANS:
            {
                return coffeBeans;
            }
            case COFFEE_BROWN:
            {
                return coffeBrown;
            }
            case COFFEE_RED:
            {
                return coffeRed;
            }
            case COFFEE_YELLOW:
            {
                return coffeYellow;
            }
            case FIRE:
            {
                return fire;
            }
            default:
                break;
        }
        return null;
        
    }

    public ImageIcon retornarIconePorValor(int valor) {
        switch (valor) {
            case 0:
                return retornarIcone(NomeIconeEnum.COFFEE_BEANS);
            case 1:
                return retornarIcone(NomeIconeEnum.COFFEE_BROWN);
            case 2:
                return retornarIcone(NomeIconeEnum.COFFEE_WHITE);
            case 3:
                return retornarIcone(NomeIconeEnum.COFFEE_YELLOW);
            case 4:
                return retornarIcone(NomeIconeEnum.COFFEE_RED);
            default:
                return null;
        }
    }

    public Icon retornarIconeExplosao() {
        return explosao;
    }

    private ImageIcon carregarIconePeca(String nomeBase) {
        URL recursoGif = getClass().getResource("images/" + nomeBase + ".gif");
        if (recursoGif != null) {
            return new ImageIcon(recursoGif);
        }

        URL recursoPng = getClass().getResource("images/" + nomeBase + ".png");
        if (recursoPng == null) {
            throw new IllegalStateException("Arquivo de imagem nao encontrado para: " + nomeBase);
        }

        ImageIcon imagemPng = new ImageIcon(recursoPng);
        Image imagemRedimensionada = imagemPng.getImage().getScaledInstance(TAMANHO_PECA, TAMANHO_PECA, java.awt.Image.SCALE_SMOOTH);
        return new ImageIcon(imagemRedimensionada);
    }

    private ImageIcon carregarIconeExplosao() {
        URL recursoConfigurado = resolverRecurso(configuracao.getRecursoAnimacaoExplosao());
        if (recursoConfigurado != null) {
            return new ImageIcon(recursoConfigurado);
        }

        URL recursoPadrao = getClass().getResource("images/grenade.gif");
        if (recursoPadrao != null) {
            return new ImageIcon(recursoPadrao);
        }

        return fire;
    }

    private URL resolverRecurso(String caminho) {
        if (caminho == null || caminho.trim().isEmpty()) {
            return null;
        }

        URL recurso = getClass().getResource(caminho);
        if (recurso != null) {
            return recurso;
        }

        if (!caminho.startsWith("/")) {
            return getClass().getResource("/" + caminho);
        }

        return null;
    }
    
}
