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
    private static final String[] ORDEM_ICONES_PECAS = {
            "coffee-beans",
            "coffee-brown",
            "coffee-white",
            "coffee-yellow",
            "coffee-red",
            "coffee-green"
    };
    private final ConfiguracaoJogo configuracao = ConfiguracaoJogo.get();
    private final ImageIcon[] iconesPecas = carregarIconesPecas();
    private final ImageIcon fire = carregarIconePeca("fire");
    private final ImageIcon explosao = carregarIconeExplosao();

    private ImageIcon[] carregarIconesPecas() {
        ImageIcon[] icones = new ImageIcon[ORDEM_ICONES_PECAS.length];
        for (int i = 0; i < ORDEM_ICONES_PECAS.length; i++) {
            icones[i] = carregarIconePeca(ORDEM_ICONES_PECAS[i]);
        }
        return icones;
    }
    
    public ImageIcon retornarIcone(NomeIconeEnum nomeIconeEnum){
        
        switch (nomeIconeEnum) {
            case COFFEE_WHITE:
            {
                return iconePorIndice(2);
            }
            case COFFEE_BEANS:
            {
                return iconePorIndice(0);
            }
            case COFFEE_BROWN:
            {
                return iconePorIndice(1);
            }
            case COFFEE_RED:
            {
                return iconePorIndice(4);
            }
            case COFFEE_YELLOW:
            {
                return iconePorIndice(3);
            }
            case COFFEE_GREEN:
            {
                return iconePorIndice(5);
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
        return iconePorIndice(valor);
    }

    public int getQuantidadePecasDisponiveis() {
        return iconesPecas.length;
    }

    private ImageIcon iconePorIndice(int indice) {
        if (indice < 0 || indice >= iconesPecas.length) {
            return null;
        }
        return iconesPecas[indice];
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
