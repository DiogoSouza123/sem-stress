# ☕ Coffee Crush (sem-stress)

> Jogo estilo **match-3** feito em **Java 8 + Swing**, com tema de cafe, fases configuraveis, animacoes, musica e progressao com desbloqueio.

<div align="center">

![Java](https://img.shields.io/badge/Java-8-orange)
![Build](https://img.shields.io/badge/Build-Ant-blue)
![UI](https://img.shields.io/badge/UI-Swing-6a5acd)
![Status](https://img.shields.io/badge/Status-Em%20Evolucao-success)

</div>

---

## ✨ Visao geral
O Coffee Crush nasceu como projeto de estudo de logica e evoluiu para um fluxo completo de jogo:
- tabuleiro dinamico
- fases configuraveis
- animacoes de explosao e cascata
- musica por fase
- menu de fases com bloqueio/desbloqueio e progresso salvo

---

## 🖼️ Galeria

### 🎬 Fluxo principal da aplicacao (menu -> fase)
![Fluxo da Aplicacao](docs/images/fluxo-completo.gif)

### 💥 Match com explosao
![Explosao de Match](docs/images/explosao-match.gif)

---

## 🎮 Funcionalidades
- ✅ Pontuacao por combo (`match-3`, `match-4`, `match-5+`)
- ✅ Resolucao em cascata com animacao
- ✅ Progressao de fases com desbloqueio
- ✅ Background e musica por fase via `.properties`
- ✅ Progresso salvo localmente
- ✅ Botao de som (ON/OFF)

---

## 🧱 Tecnologias
- Java 8
- Apache Ant
- Java Swing
- JLayer (suporte a MP3)
- JUnit 4 + Hamcrest

---

## 📁 Estrutura do projeto
- `src/com/semstress/` classes principais e telas
- `src/com/semstress/images/` assets visuais
- `src/com/semstress/audio/` musicas e sons
- `src/com/semstress/configuracao-jogo.properties` configuracao base
- `src/com/semstress/fases.properties` catalogo e parametros das fases
- `save/progresso-fases.properties` progresso do jogador (gerado em runtime)
- `build.xml` build e execucao com Ant

---

## 🛠️ Pre-requisitos
- JDK 8 (`java` e `javac` no PATH)
- Apache Ant (`ant` no PATH)
- Bibliotecas em `lib/` (`jlayer`, `junit`, `hamcrest`)

---

## ⚙️ Instalacao pelo terminal (Windows / PowerShell)

### 1) Instalar Java 8 (Amazon Corretto)
```powershell
winget install -e --id Amazon.Corretto.8.JDK
java -version
javac -version
```

### 2) Instalar Apache Ant (fonte oficial Apache)
```powershell
$ver = "1.10.15"
$zip = "$env:TEMP\apache-ant-$ver-bin.zip"
$url = "https://archive.apache.org/dist/ant/binaries/apache-ant-$ver-bin.zip"
$dest = "C:\Tools"

New-Item -ItemType Directory -Force -Path $dest | Out-Null
Invoke-WebRequest -Uri $url -OutFile $zip
Expand-Archive -Path $zip -DestinationPath $dest -Force

$antHome = "$dest\apache-ant-$ver"
[Environment]::SetEnvironmentVariable("ANT_HOME", $antHome, "User")
$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($userPath -notlike "*$antHome\bin*") {
  [Environment]::SetEnvironmentVariable("Path", "$userPath;$antHome\bin", "User")
}
```

Feche e abra o terminal, depois valide:
```powershell
ant -version
```

### 3) Baixar libs externas (Maven Central)
Na raiz do projeto:
```powershell
New-Item -ItemType Directory -Force -Path .\lib | Out-Null
Invoke-WebRequest "https://repo1.maven.org/maven2/javazoom/jlayer/1.0.1/jlayer-1.0.1.jar" -OutFile ".\lib\jlayer-1.0.1.jar"
Invoke-WebRequest "https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar" -OutFile ".\lib\junit-4.13.2.jar"
Invoke-WebRequest "https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar" -OutFile ".\lib\hamcrest-core-1.3.jar"
```

---

## ▶️ Como rodar
Na raiz do repositorio:

```powershell
ant clean
ant compile
ant run
```

---

## 📦 Gerar JAR
```powershell
ant jar
java -jar .\dist\SemStress1.0.jar
```

---

## 🧪 Rodar testes
```powershell
ant test
```

---

## 🎚️ Configuracao de fases
O arquivo `src/com/semstress/fases.properties` controla as fases.

Exemplo:
```properties
fase.3.nome=Fase 3 - Torrefacao
fase.3.tabuleiro.linhas=6
fase.3.tabuleiro.colunas=6
fase.3.jogo.movimentos_iniciais=17
fase.3.jogo.meta_pontos=9000
fase.3.ui.recurso_background=/com/semstress/images/background.gif
fase.3.audio.recurso_musica_fundo=/com/semstress/audio/fur-elise.wav
fase.3.audio.volume_percentual=70
```

Tambem e possivel sobrescrever por `config/fases.properties` sem recompilar.

---

## 👨‍💻 Autor
Desenvolvido por **Diogo Souza**, com foco em aprendizado pratico e evolucao continua. 🙌
