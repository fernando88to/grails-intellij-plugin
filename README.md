# Apache Grails IntelliJ Plugin

IntelliJ IDEA plugin for the [Apache Grails](https://grails.apache.org/) framework:
GSP language support (parsing, highlighting, completion, refactoring), Grails project
structure and navigation, run configurations, taglib/domain-class support, and
integrations for i18n, coverage, Hibernate, Maven, and language injection.

This codebase was originally developed by JetBrains s.r.o. and imported from
[JetBrains/intellij-obsolete-plugins](https://github.com/JetBrains/intellij-obsolete-plugins).

## Requirements

- IntelliJ IDEA Ultimate 2026.2+ (`sinceBuild` 262)
- Java 25 (the `grails-rt`, `grails-compiler-patch`, and `jps-plugin` modules target Java 8/11)

## Building

```
./gradlew buildPlugin
```

The plugin ZIP is written to `build/distributions/`.

Other useful tasks:

```
./gradlew check          # compile and run tests
./gradlew verifyPlugin   # IntelliJ Plugin Verifier
./gradlew rat            # Apache RAT license audit
./gradlew runIde         # launch a sandbox IDE with the plugin
```

## Branch `custom`

Esta branch do git chamada `custom` é uma versão customizada com as seguintes funcionalidades:

- **Fix Grails application detection race on project startup** — correção de uma condição de corrida (race condition) na detecção da aplicação Grails durante a inicialização do projeto.
- Na barra superior de navegação entre os artefatos de um domínio, antes a pesquisa do domínio/controller/service/view e test exigia que eles estivessem no mesmo pacote; agora basta que tenham o mesmo nome.
- Ainda nessa barra superior de navegação, foram introduzidos atalhos com `Alt+1` (numérico) para ir para domínios, `Alt+2` (numérico) para ir para controllers, e assim por diante para service, view e test.
- Novo botão **Data Service** na barra de navegação, com atalho `Alt+6` (numérico): pesquisa como o service, mas pelo sufixo `DataService`. Por exemplo, a partir do domínio `OrgaoJudiciario` ele localiza `OrgaoJudiciarioDataService`.
- O botão **Tests** passou a localizar os testes apenas pelo nome da classe, sem exigir o mesmo pacote, e aceita todos os sufixos de teste conhecidos (`Spec`, `IntegrationSpec`, `Test`, `UnitSpec` etc.) em projetos Grails 3+, e não só `Spec`. Isso cobre projetos que separam testes unitários dos de integração pelo sufixo.

## Links

- [Apache Grails](https://grails.apache.org/)
- [Issue tracker](https://github.com/apache/grails-intellij-plugin/issues)
- [Mailing lists](https://grails.apache.org/community/#mailing-lists)

## License

Apache License, Version 2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).
