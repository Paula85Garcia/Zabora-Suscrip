# 🧪 GUÍA DE TESTS - ZABORA SUBSCRIPTION SERVICE

## 📋 Tabla de Contenidos

1. [Tests Unitarios](#tests-unitarios)
2. [Tests de Integración](#tests-de-integración)
3. [Tests End-to-End](#tests-end-to-end)
4. [Mocking y Fixtures](#mocking-y-fixtures)
5. [Ejecución y Reportes](#ejecución-y-reportes)
6. [Troubleshooting](#troubleshooting)

## 🧪 Tests Unitarios

### SuscripcionServicioTest

```bash
# Ejecutar tests del servicio
mvn test -Dtest=SuscripcionServicioTest

# Tests principales:
- crearSuscripcion_ConPlanValido_DebeRetornarExito()
- crearSuscripcion_ConPlanInvalido_DebeRetornarError()
- obtenerSuscripcion_ConUsuarioValido_DebeRetornarSuscripcion()
- cancelarSuscripcion_ConSuscripcionActiva_DebeRetornarExito()
```

### SuscripcionControllerTest

```bash
# Ejecutar tests del controlador
mvn test -Dtest=SuscripcionControllerTest

# Tests principales:
- getPlanes_DebeRetornarListaDePlanes()
- suscribirse_ConDatosValidos_DebeRetornar200()
- suscribirse_ConDatosInvalidos_DebeRetornar400()
- suscribirse_SinHeaders_DebeRetornar401()
- obtenerEstado_DebeRetornarEstadoDeSuscripcion()
```

### PagoServicioTest

```bash
# Ejecutar tests de pagos
mvn test -Dtest=PagoServicioTest

# Tests principales:
- crearPago_ConTarjeta_DebeRetornarPreferencia()
- crearPago_ConPSE_DebeRetornarPreferencia()
- procesarPago_ConPagoAprobado_DebeActivarSuscripcion()
- procesarPago_ConPagoRechazado_DebeMantenerPendiente()
```

## 🔧 Tests de Integración

### Configuración para Tests

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

mercadopago:
  environment: sandbox
  access-token: test_token
```

### IntegrationTest

```bash
# Ejecutar tests de integración
mvn verify -Pintegration-test

# Tests principales:
- getEstado_SinHeaders_DebeRetornar400()
- suscribirse_ConDatosValidos_DebeRetornar200()
- suscribirse_SinHeaders_DebeRetornar400()
```

### WebhookIntegrationTest

```bash
# Tests de webhooks con MercadoPago
mvn test -Dtest=WebhookIntegrationTest

# Tests principales:
- recibirWebhook_ConPagoAprobado_DebeActivarSuscripcion()
- recibirWebhook_ConPagoRechazado_DebeMantenerPendiente()
- recibirWebhook_ConDatosInvalidos_DebeRetornarError()
```

## 🌐 Tests End-to-End

### Flujo Completo de Suscripción

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(OrderAnnotation.class)
class SuscripcionE2ETest {

    @Test
    @Order(1)
    void flujoCompletoSuscripcion() {
        // 1. Obtener planes disponibles
        ResponseEntity<PlanResponse> planes = testRestTemplate.getForEntity(
            "/api/suscripciones/planes", PlanResponse.class);
        assertEquals(200, plans.getStatusCode());

        // 2. Crear suscripción
        CrearSuscripcionRequest request = CrearSuscripcionRequest.builder()
            .planId("premium")
            .usuarioId(123)
            .metodoPago("tarjeta")
            .build();

        ResponseEntity<SuscripcionResponse> response = testRestTemplate.postForEntity(
            "/api/suscripciones/crear", request, SuscripcionResponse.class);
        assertEquals(200, response.getStatusCode());

        // 3. Simular webhook de aprobación
        WebhookPayload webhook = WebhookPayload.builder()
            .type("payment")
            .paymentId("12345")
            .status("approved")
            .build();

        ResponseEntity<String> webhookResponse = testRestTemplate.postForEntity(
            "/api/webhooks/mercadopago", webhook, String.class);
        assertEquals(200, webhookResponse.getStatusCode());

        // 4. Verificar estado final
        ResponseEntity<EstadoSuscripcionResponse> estado = testRestTemplate.getForEntity(
            "/api/suscripciones/estado?usuarioId=123", EstadoSuscripcionResponse.class);
        assertEquals("ACTIVA", estado.getBody().getEstado());
    }
}
```

### Tests de PSE

```java
@Test
void flujoPagoPSE() {
    // 1. Crear pago PSE
    PseRequest request = PseRequest.builder()
        .planId("premium")
        .usuarioId(123)
        .banco("1022")
        .tipoPersona("natural")
        .documento("123456789")
        .build();

    ResponseEntity<PagoResponse> response = pagoController.crearPagoPse(request);
    assertEquals(200, response.getStatusCode());

    // 2. Verificar URL de redirección
    assertNotNull(response.getBody().getRedirectUrl());
    assertTrue(response.getBody().getRedirectUrl().contains("pse"));

    // 3. Simular retorno del banco
    PseRetornoRequest retorno = PseRetornoRequest.builder()
        .paymentId("pse_123")
        .estado("aprobado")
        .build();

    ResponseEntity<String> retornoResponse = pagoController.procesarRetornoPSE(retorno);
    assertEquals(200, retornoResponse.getStatusCode());
}
```

## 🎭 Mocking y Fixtures

### Mock de MercadoPago

```java
@ExtendWith(MockitoExtension.class)
class PagoServicioTest {

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private PagoServicio pagoServicio;

    @Test
    void crearPago_DebeLlamarMercadoPago() {
        // Arrange
        PagoRequest request = PagoRequest.builder()
            .planId("premium")
            .build();

        Payment paymentMock = new Payment();
        paymentMock.setId(123L);
        paymentMock.setInitPoint("https://mercadopago.com/checkout");

        when(paymentClient.create(any(PreferenceRequest.class)))
            .thenReturn(paymentMock);

        // Act
        PagoResponse response = pagoServicio.crearPago(request);

        // Assert
        assertEquals("https://mercadopago.com/checkout", response.getRedirectUrl());
        verify(paymentClient, times(1)).create(any(PreferenceRequest.class));
    }
}
```

### Mock de AuthClient

```java
@ExtendWith(MockitoExtension.class)
class ServicioNotificacionesTest {

    @Mock
    private AuthClient authClient;

    @InjectMocks
    private ServicioNotificaciones servicioNotificaciones;

    @Test
    void notificarActualizacionSuscripcion_DebeLlamarAuthClient() {
        // Act
        servicioNotificaciones.notificarActualizacionSuscripcion(123, "PREMIUM");

        // Assert
        verify(authClient, times(1)).actualizarRolPremium(123);
    }
}
```

### Fixtures de Datos

```java
public class TestDataFactory {

    public static UsuarioSuscripcion crearSuscripcionActiva() {
        return UsuarioSuscripcion.builder()
            .id("sub_123")
            .usuarioId(123)
            .estado(EstadoSuscripcion.ACTIVA)
            .plan(crearPlanPremium())
            .inicioPeriodoActual(LocalDateTime.now())
            .finPeriodoActual(LocalDateTime.now().plusDays(30))
            .build();
    }

    public static PlanSuscripcion crearPlanPremium() {
        return PlanSuscripcion.builder()
            .id("premium")
            .nombre("Premium")
            .precio(BigDecimal.valueOf(29.99))
            .moneda("USD")
            .build();
    }

    public static Pago crearPagoCompletado() {
        return Pago.builder()
            .id("pago_123")
            .suscripcionId("sub_123")
            .estado(EstadoPago.COMPLETADO)
            .monto(BigDecimal.valueOf(29.99))
            .fechaPago(LocalDateTime.now())
            .build();
    }
}
```

## 📊 Ejecución y Reportes

### Comandos de Ejecución

```bash
# Todos los tests unitarios
mvn test

# Tests específicos
mvn test -Dtest="*Suscripcion*"
mvn test -Dtest="*Pago*"
mvn test -Dtest="*Webhook*"

# Tests con cobertura
mvn clean test jacoco:report

# Tests de integración
mvn verify -Pintegration-test

# Tests con perfil específico
mvn test -Dspring.profiles.active=test

# Tests en paralelo
mvn test -T 4

# Saltar tests lentos
mvn test -DskipSlowTests=true
```

### Reportes Generados

```
target/
├── surefire-reports/
│   ├── TEST-com.zabora.subscription.SuscripcionServicioTest.xml
│   ├── TEST-com.zabora.subscription.SuscripcionControllerTest.xml
│   └── TEST-com.zabora.subscription.PagoServicioTest.xml
├── failsafe-reports/
│   └── TEST-com.zabora.subscription.IntegrationTest.xml
├── site/
│   └── jacoco/
│       ├── index.html
│       └── com.zabora.subscription/
└── test-classes/
```

### Cobertura de Código

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## 🐛 Troubleshooting

### Problemas Comunes en Tests

#### 1. Error de Conexión a Base de Datos

```bash
# Verificar configuración
cat src/test/resources/application-test.yml

# Usar H2 en memoria
spring:
  datasource:
    url: jdbc:h2:mem:testdb
```

#### 2. Mocks no Funcionan

```java
// Verificar anotaciones
@ExtendWith(MockitoExtension.class)
@Mock private AuthClient authClient;

// Inicializar mocks
@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
}
```

#### 3. Tests Lentos

```bash
# Configurar timeout
@Test(timeout = 5000)
void testLento() {
    // test code
}

# Usar @DirtiesContext
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
```

#### 4. Problemas con Webhooks

```java
// Mock de PaymentClient
@MockBean
private PaymentClient paymentClient;

// Simular respuesta
when(paymentClient.get(anyLong()))
    .thenReturn(createPaymentMock());
```

### Debug de Tests

```bash
# Ejecutar con debug
mvn test -Dmaven.surefire.debug

# Ver logs específicos
mvn test -Dlogging.level.com.zabora.subscription=DEBUG

# Ejecutar test específico con debug
mvn test -Dtest=SuscripcionServicioTest#crearSuscripcion -Dmaven.surefire.debug
```

### Configuración de CI/CD

```yaml
# .github/workflows/test.yml
name: Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run tests
        run: mvn clean test
      - name: Generate report
        run: mvn jacoco:report
      - name: Upload coverage
        uses: codecov/codecov-action@v3
```

## 📝 Mejores Prácticas

1. **Nombres descriptivos**: `crearSuscripcion_ConPlanValido_DebeRetornarExito`
2. **Arrange-Act-Assert**: Estructura clara en cada test
3. **Independencia**: Tests no deben depender unos de otros
4. **Datos de prueba**: Usar factories para datos consistentes
5. **Mocks**: Simular dependencias externas
6. **Cobertura**: Mantener >80% de cobertura
7. **Limpieza**: Usar @AfterEach para limpiar estado

## 🚀 Ejecución Rápida

```bash
# Comando completo para ejecutar todos los tests
mvn clean test jacoco:report && \
  echo "Tests completados. Ver reportes en target/site/jacoco/index.html"
```
