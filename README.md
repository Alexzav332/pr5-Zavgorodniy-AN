# Практическая работа №5 (вариант 12)

Строки, массивы, циклы и методы обработки данных: анализ табеля (7 значений, пн–вс).

## Сборка и запуск
```
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out ru.edu.pr02.SelfCheck
```
Windows: `run_selfcheck.bat`, Unix: `run_selfcheck.sh` (запускать из корня проекта – нужен `data/sample.csv`).

Режимы SelfCheck: без аргументов – unit, data-driven, затем интерактивный (выход: `exit` или пустая строка).
Отдельно: `java -cp out ru.edu.pr02.SelfCheck unit|data|interactive`.

## Диаграммы
```
dot -Tpng activity.dot -o activity.png   # из каталога diagrams
dot -Tpng class.dot -o class.png
```
