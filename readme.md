# Ultimate Leveling Based on Mumford-Shah Energy Functional Applied to Plant Detection

This repository contains the source code and executable rotines to test our implementation in plant bounding box detection task applied in [Minervi Plant Dataset](https://www.plant-phenotyping.org/datasets-overview). By the way, this work was presented in [CIARP-2017](https://www.ciarp2017.org/) and published in Lecture Notes in Computer Science.

![](others/ara2012_tray01_BOUNDS.png)

## What do we need to start?

Some softwares and libraries used here are listed bellow:

```
eclipse >= Kepler
jdk >= 1.7
matlab >= r2014b
imageJ >= 1.50i (to attend the user)
mmlib4j >= 1.0 (provided in this repository)
```

## Folders structure

The project was made using Eclipse IDE, so the structure was adapted to port the [matlab evaluation tool](https://www.plant-phenotyping.org/lw_resource/datapool/_items/item_464/evaluationsuite_v0.2.zip) provided by Minervi.

```
src -> *.java files
bin -> *.class files
datasets -> folder to put plant dataset
evaluation -> folder to save results using the matlab evaluation tool
experiments -> contains extracted dataset by call: extractdataset.m (you can choose this path)
libraries -> mmlib4j-api-1.1.jar
matlab -> all matlab implementations of the matlab evaluation tool
means -> a folder to save extracted means of multiple tests to draw a robustness graph
results -> contains all results (you can choose this path)
others -> somethings to attend this readme.md file
```

## Using the codes

We divide the necessary to use this work in two categories.

### Java implementations

### Matlab tool

## Results

## Autors

The paper was produced by Charles Gobber, Wonder Alexandre Luz Alves and Ronaldo Fumio Hashimoto. This repository was created by Charles Gobber and you can send me a mail if you need something:

* **Charles Gobber** - charles26f@gmail.com
