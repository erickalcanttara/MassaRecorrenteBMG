# AGENTS.md

## Project at a glance
- Maven Java 15 project focused on **test automation/data generation**, not app runtime (`src/main/java` is empty).
- Core behavior lives in `src/test/java` and orchestrates external APIs + SQL Server.
- Main use case: generate recurring test mass (individual -> account -> card -> virtual card -> unlock).

## Code map (start here)
- `src/test/java/individuals/GenarateAccountAndCardTest.java`: end-to-end REST flow and token acquisition.
- `src/test/java/queries/IndividualsUtils.java`: individual payload JSON (Faker `pt-BR` + CPF/RG generation).
- `src/test/java/queries/AccountUtils.java`: account payload JSON.
- `src/test/java/queries/CardsUtils.java`: card payload JSON.
- `src/test/java/queries/GenerateCpfCnpjRg.java`: CPF/CNPJ/RG generators and validators.
- `src/test/java/queries/DatabaseProperties.java` + `BDconnect.java`: YAML config loading and JDBC connection.
- `src/test/java/queries/MassaRecorrenteQueries.java`: SQL query (`SELECT TOP 1 * FROM CONTAS ORDER BY 1 DESC`).
- `src/test/java/massaRecorrente/MassaRecorrenteClass.java`: DB query smoke test and JSON print.

## Data flow and boundaries
- REST flow in `GenarateAccountAndCardTest` extracts IDs from each response and feeds next calls (`id_pessoa`, `idContaCredito`, `idCartaoVitual`).
- API boundary: Caradhras sandbox/auth endpoints are hardcoded in test class methods.
- DB boundary: SQL Server connection info comes from `src/test/resources/application.yaml` (`database.sql_server`).

## Local workflows
- Run all tests:
```bash
mvn test
```
- Run API mass generation only:
```bash
mvn -Dtest=individuals.GenarateAccountAndCardTest test
```
- Run DB query test only:
```bash
mvn -Dtest=massaRecorrente.MassaRecorrenteClass test
```

## Project-specific conventions
- Payloads are built as raw JSON strings (no POJOs/builders); preserve key names/shape when editing.
- Test classes use mixed frameworks (`org.junit.jupiter.api.Test`, `org.junit.Test`, and `org.testng.SkipException`).
- Naming is mostly Portuguese and contains legacy typos (`Genarate...`, `idCartaoVitual`, `BDconnect`); avoid broad renames unless requested.
- `application.yaml` contains environment credentials/tokens; do not add new secrets in code.
- RestAssured pattern is consistent: `given() -> headers/body -> post() -> statusCode() -> extract().response()`.

## Dependency/integration notes
- Key deps in `pom.xml`: Rest-Assured, SnakeYAML, Jackson, SQL Server JDBC, JavaFaker, JUnit 4/5, TestNG.
- `testng` and `junit` appear in multiple scopes; avoid dependency cleanup unless task explicitly asks for build refactor.
- Network access is required for real test execution (external auth/API + SQL Server host).

