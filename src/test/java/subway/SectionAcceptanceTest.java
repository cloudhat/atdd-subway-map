package subway;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import subway.CommonStep.DatabaseCleanup;
import subway.CommonStep.LineStep;
import subway.CommonStep.SectionStep;
import subway.CommonStep.StationStep;


import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("지하철 구간 관련 기능")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class SectionAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    private Long 이호선;
    private Long 강남역;
    private Long 역삼역;
    private Long 선릉역;
    private Long 삼성역;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
        databaseCleanup.execute();

        이호선 =  LineStep.지하철_노선_생성( "2호선", "green").jsonPath().getLong("id");
        강남역 =  StationStep.지하철역_생성("강남역").jsonPath().getLong("id");
        역삼역 =  StationStep.지하철역_생성("역삼역").jsonPath().getLong("id");
        선릉역 =  StationStep.지하철역_생성("선릉역").jsonPath().getLong("id");
        삼성역 =  StationStep.지하철역_생성("삼성역").jsonPath().getLong("id");
    }


    /**
     * Given 지하철 역과 노선을 생성하고
     * When 해당 노선에 구간을 추가하면
     * Then 해당 노선 조회 시 추가된 구간을 확인할 수 있다
     */
    @DisplayName("지하철 구간을 생성한다.")
    @Test
    void createSection() {

        // when
        ExtractableResponse<Response> response = SectionStep.지하철구간_생성(이호선,강남역,역삼역,10L);

        //then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        JsonPath lineJsonPath = LineStep.지하철_노선_조회(이호선);
        assertThat(lineJsonPath.getList("stations.id", Long.class)).containsExactly(강남역,역삼역);

    }


    /**
     * Given 지하철 역과 노선을 생성하고
     * When 해당 노선에 등록되어있는 하행 종점역이 아닌 상행역을 가진 구간을 추가하면
     * Then Bad Request 400 error가 발생한다
     */
    @DisplayName("잘못된 상행역을 가진 지하철 구간을 생성한다.")
    @Test
    void createInvalidSectionDueToUpStation() {
        //given
        SectionStep.지하철구간_생성(이호선,강남역,역삼역,10L);

        // when
        ExtractableResponse<Response> response = SectionStep.지하철구간_생성(이호선,선릉역,삼성역,20L);

        //then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    /**
     * Given 지하철 역과 노선을 생성하고
     * When  해당 노선에 등록되어있는 역과 동일한 하행역을 가진 구간을 추가하면
     * Then Bad Request 400 error가 발생한다
     */

    @DisplayName("잘못된 하행역을 가진 구간을 생성한다.")
    @Test
    void createInvalidSectionDueToDownStation() {
        //given
        SectionStep.지하철구간_생성(이호선,강남역,역삼역,10L);

        // when
        ExtractableResponse<Response> response = SectionStep.지하철구간_생성(이호선,역삼역,강남역,20L);

        //then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    /**
     * Given 지하철 역,노선, 구간을 생성하고
     * When 생성한 구간을 삭제하면
     * Then 해당 노선 조회 시 삭제된 노선을 조회할 수 없다
     */
    @DisplayName("지하철 구간을 삭제한다")
    @Test
    void deleteSection() {
        //given
        SectionStep.지하철구간_생성(이호선,강남역,역삼역,10L);
        SectionStep.지하철구간_생성(이호선,역삼역,선릉역,20L);
        JsonPath lineJsonPathBefore = LineStep.지하철_노선_조회(이호선);

        // when
        ExtractableResponse<Response> response = SectionStep.지하철구간_삭제(이호선, 역삼역);
        JsonPath lineJsonPathAfter = LineStep.지하철_노선_조회(이호선);

        //then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    /**
     * Given 지하철 역,노선, 구간을 생성하고
     * When 지하철 노선에 등록된 마지막 구간이 아닌 구간을 삭제시도할 경우
     * Then  Bad Request 400 error가 발생한다
     */
    @DisplayName("지하철 노선에 등록된 마지막 구간이 아닌 구간을 삭제한다")
    @Test
    void invalidDeleteSectionNotLastSection() {
        //given
        SectionStep.지하철구간_생성(이호선,강남역,역삼역,10L);
        SectionStep.지하철구간_생성(이호선,역삼역,선릉역,20L);

        // when
        ExtractableResponse<Response> response = SectionStep.지하철구간_삭제(이호선, 강남역);

        //then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    /**
     * Given 지하철 역,노선, 구간을 생성하고
     * When 지하철 노선에 등록된 구간이 1개인 경우에 구간을 삭제시도할 경우
     * Then  Bad Request 400 error가 발생한다
     */
    @DisplayName("지하철 노선에 등록된 구간이 1개인 경우에 구간을 삭제시도한다")
    @Test
    void invalidDeleteSectionOnlyOneSection() {
        //given
        SectionStep.지하철구간_생성(이호선,강남역,역삼역,10L);

        // when
        ExtractableResponse<Response> response = SectionStep.지하철구간_삭제(이호선, 강남역);

        //then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }
}
