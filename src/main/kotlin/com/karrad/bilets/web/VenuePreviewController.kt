package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.dto.BoundsDto
import com.karrad.bilets.domain.dto.SeatLayoutDto
import com.karrad.bilets.domain.dto.SectionRenderDto
import com.karrad.bilets.domain.dto.StageRenderDto
import com.karrad.bilets.domain.dto.VenueRenderDto
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.Section
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.entity.VenueStruct
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class VenuePreviewController(
    private val objectMapper: ObjectMapper
) {

    @GetMapping("/", produces = [MediaType.TEXT_HTML_VALUE])
    fun root(): String {
        val venue = demoVenue()
        val layoutTemplate = demoLayoutTemplate(venue.spaces.first().id)
        val renderLayout = demoRenderLayout()

        val venueJson = objectMapper.writeValueAsString(VenueStruct(sections = layoutTemplate.sections))
        val renderJson = objectMapper.writeValueAsString(renderLayout)

        return """
            <!DOCTYPE html>
            <html lang="ru">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>${venue.label}</title>
              <style>
                :root {
                  --bg: #0f172a;
                  --panel: #111827;
                  --panel-2: #1f2937;
                  --line: #334155;
                  --text: #e5e7eb;
                  --muted: #94a3b8;
                  --seat: #22c55e;
                  --seat-hover: #4ade80;
                  --seat-stroke: #166534;
                  --section: rgba(59, 130, 246, 0.12);
                  --section-border: #60a5fa;
                }

                * { box-sizing: border-box; }

                body {
                  margin: 0;
                  font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                  background: linear-gradient(180deg, #020617 0%, #0f172a 100%);
                  color: var(--text);
                  min-height: 100vh;
                }

                .app {
                  width: min(1200px, 100%);
                  margin: 0 auto;
                  padding: 16px;
                  display: grid;
                  gap: 12px;
                  min-height: 100vh;
                }

                .toolbar, .legend {
                  background: rgba(17, 24, 39, 0.92);
                  border: 1px solid var(--line);
                  border-radius: 14px;
                  padding: 12px;
                  display: flex;
                  gap: 8px;
                  flex-wrap: wrap;
                  align-items: center;
                }

                .toolbar strong { margin-right: 8px; }

                button {
                  background: var(--panel-2);
                  color: var(--text);
                  border: 1px solid var(--line);
                  border-radius: 10px;
                  padding: 8px 12px;
                  cursor: pointer;
                }

                .viewer {
                  background: rgba(17, 24, 39, 0.92);
                  border: 1px solid var(--line);
                  border-radius: 18px;
                  overflow: hidden;
                  position: relative;
                  min-height: 560px;
                }

                .info {
                  position: absolute;
                  right: 12px;
                  top: 12px;
                  background: rgba(2, 6, 23, 0.85);
                  border: 1px solid var(--line);
                  border-radius: 12px;
                  padding: 10px 12px;
                  font-size: 13px;
                  max-width: 280px;
                  z-index: 2;
                }

                .muted { color: var(--muted); font-size: 12px; }
                svg { display: block; width: 100%; height: 100%; touch-action: none; user-select: none; }
                .section { fill: var(--section); stroke: var(--section-border); stroke-width: 2; }
                .section-label { fill: var(--text); font-size: 16px; font-weight: 700; text-anchor: middle; dominant-baseline: middle; }
                .row-label { fill: var(--muted); font-size: 10px; text-anchor: end; dominant-baseline: middle; }
                .seat { fill: var(--seat); stroke: var(--seat-stroke); stroke-width: 1; cursor: pointer; }
                .seat:hover { fill: var(--seat-hover); }
                .seat.selected { fill: #f59e0b; stroke: #b45309; }
                .stage { fill: rgba(244, 114, 182, 0.15); stroke: #f472b6; stroke-width: 2; }
                .stage-label { fill: #f9a8d4; font-size: 14px; font-weight: 700; text-anchor: middle; dominant-baseline: middle; }
              </style>
            </head>
            <body>
              <div class="app">
                <div class="toolbar">
                  <strong>${venue.label}</strong>
                  <button id="fitBtn">Fit</button>
                  <button id="zoomInBtn">+</button>
                  <button id="zoomOutBtn">-</button>
                  <span class="muted">Город: ${venue.city.label}. Данные схемы и layout пришли с backend.</span>
                </div>
                <div class="legend">
                  <span>Свободно: зеленый</span>
                  <span>Выбрано: оранжевый</span>
                  <span>Секции: голубой контур</span>
                </div>
                <div class="viewer" id="viewer">
                  <div class="info">
                    <div><strong>Выбор:</strong> <span id="selectionTitle">ничего</span></div>
                    <div class="muted" id="selectionMeta">Нажми на место</div>
                  </div>
                  <svg id="svg" viewBox="0 0 1000 1000" preserveAspectRatio="xMidYMid meet"></svg>
                </div>
              </div>

              <script>
                const venueStruct = $venueJson;
                const renderLayout = $renderJson;
                const SCENE_SIZE = 1000;
                const svg = document.getElementById("svg");
                const viewer = document.getElementById("viewer");
                const infoTitle = document.getElementById("selectionTitle");
                const infoMeta = document.getElementById("selectionMeta");

                let zoom = 1;
                let baseScale = 1;
                let translateX = 0;
                let translateY = 0;
                let selectedSeatId = null;
                let dragState = null;

                function normToScene(value) { return value * SCENE_SIZE; }
                function currentScale() { return baseScale * zoom; }
                function clamp(value, min, max) { return Math.min(max, Math.max(min, value)); }

                function buildSeatModels() {
                  return renderLayout.sections.flatMap((sectionLayout) => {
                    const section = venueStruct.sections.find((item) => item.key === sectionLayout.sectionKey);
                    const bounds = sectionLayout.bounds;
                    const rules = sectionLayout.seatLayout || {};
                    const x = normToScene(bounds.x);
                    const y = normToScene(bounds.y);
                    const width = normToScene(bounds.width);
                    const height = normToScene(bounds.height);
                    const padding = width * (rules.paddingRatio ?? 0.08);
                    const innerX = x + padding;
                    const innerY = y + padding;
                    const innerWidth = width - padding * 2;
                    const innerHeight = height - padding * 2;
                    const rowCount = section.rows.length;
                    const rowGap = rowCount > 1 ? (innerHeight * (rules.rowGapRatio ?? 0.12)) / rowCount : 0;
                    const rowSlotHeight = (innerHeight - rowGap * Math.max(0, rowCount - 1)) / rowCount;

                    return section.rows.flatMap((row, rowIndex) => {
                      const seatCount = row.endSeat - row.startSeat + 1;
                      const seatGapWeight = rules.seatGapRatio ?? 0.03;
                      const seatGapFactor = Math.max(0.18, seatGapWeight * 5);
                      const seatSize = Math.min(
                        rowSlotHeight * 0.72,
                        innerWidth / (seatCount + Math.max(0, seatCount - 1) * seatGapFactor)
                      );
                      const seatGap = seatSize * seatGapFactor;
                      const rowY = innerY + rowIndex * (rowSlotHeight + rowGap) + rowSlotHeight / 2;
                      const rowWidth = seatCount * seatSize + Math.max(0, seatCount - 1) * seatGap;
                      const startX = innerX + (innerWidth - rowWidth) / 2;

                      return Array.from({ length: seatCount }, (_, index) => {
                        const seatNumber = row.startSeat + index;
                        return {
                          id: `${'$'}{section.key}:${'$'}{row.key}:${'$'}{seatNumber}`,
                          label: `${'$'}{section.label}, ${'$'}{row.label}, место ${'$'}{seatNumber}`,
                          price: row.price,
                          cx: startX + index * (seatSize + seatGap) + seatSize / 2,
                          cy: rowY,
                          r: Math.max(6, seatSize / 2)
                        };
                      });
                    });
                  });
                }

                function fitToViewer() {
                  const rect = viewer.getBoundingClientRect();
                  const scale = Math.min((rect.width * 0.9) / SCENE_SIZE, (rect.height * 0.75) / SCENE_SIZE);
                  baseScale = scale;
                  zoom = 1;
                  translateX = (rect.width - SCENE_SIZE * scale) / 2;
                  translateY = (rect.height - SCENE_SIZE * scale) / 2;
                  applyTransform();
                }

                function applyTransform() {
                  svg.style.transformOrigin = "0 0";
                  svg.style.transform = `translate(${'$'}{translateX}px, ${'$'}{translateY}px) scale(${'$'}{currentScale()})`;
                }

                function zoomAt(nextZoom, anchorX, anchorY) {
                  const clampedZoom = clamp(nextZoom, 0.5, 4);
                  const prevScale = currentScale();
                  const nextScale = baseScale * clampedZoom;

                  if (prevScale === nextScale) return;

                  const sceneX = (anchorX - translateX) / prevScale;
                  const sceneY = (anchorY - translateY) / prevScale;

                  zoom = clampedZoom;
                  translateX = anchorX - sceneX * nextScale;
                  translateY = anchorY - sceneY * nextScale;
                  applyTransform();
                }

                function zoomFromViewerCenter(factor) {
                  const rect = viewer.getBoundingClientRect();
                  zoomAt(zoom * factor, rect.width / 2, rect.height / 2);
                }

                function render() {
                  const seatModels = buildSeatModels();
                  svg.innerHTML = "";

                  if (renderLayout.stage) {
                    const stage = renderLayout.stage;
                    const stageRect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
                    stageRect.setAttribute("x", normToScene(stage.x));
                    stageRect.setAttribute("y", normToScene(stage.y));
                    stageRect.setAttribute("width", normToScene(stage.width));
                    stageRect.setAttribute("height", normToScene(stage.height));
                    stageRect.setAttribute("rx", 16);
                    stageRect.setAttribute("class", "stage");
                    svg.appendChild(stageRect);

                    const stageLabel = document.createElementNS("http://www.w3.org/2000/svg", "text");
                    stageLabel.setAttribute("x", normToScene(stage.x + stage.width / 2));
                    stageLabel.setAttribute("y", normToScene(stage.y + stage.height / 2));
                    stageLabel.setAttribute("class", "stage-label");
                    stageLabel.textContent = stage.label;
                    svg.appendChild(stageLabel);
                  }

                  renderLayout.sections.forEach((sectionLayout) => {
                    const section = venueStruct.sections.find((item) => item.key === sectionLayout.sectionKey);
                    const { x, y, width, height } = sectionLayout.bounds;
                    const sectionX = normToScene(x);
                    const sectionY = normToScene(y);
                    const sectionW = normToScene(width);
                    const sectionH = normToScene(height);

                    const rect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
                    rect.setAttribute("x", sectionX);
                    rect.setAttribute("y", sectionY);
                    rect.setAttribute("width", sectionW);
                    rect.setAttribute("height", sectionH);
                    rect.setAttribute("rx", 16);
                    rect.setAttribute("class", "section");
                    svg.appendChild(rect);

                    const label = document.createElementNS("http://www.w3.org/2000/svg", "text");
                    label.setAttribute("x", sectionX + sectionW / 2);
                    label.setAttribute("y", sectionY + 24);
                    label.setAttribute("class", "section-label");
                    label.textContent = section.label;
                    svg.appendChild(label);
                  });

                  seatModels.forEach((seat) => {
                    const circle = document.createElementNS("http://www.w3.org/2000/svg", "circle");
                    circle.setAttribute("cx", seat.cx);
                    circle.setAttribute("cy", seat.cy);
                    circle.setAttribute("r", seat.r);
                    circle.setAttribute("class", `seat${'$'}{selectedSeatId === seat.id ? " selected" : ""}`);
                    circle.addEventListener("click", (event) => {
                      event.stopPropagation();
                      selectedSeatId = seat.id;
                      infoTitle.textContent = seat.label;
                      infoMeta.textContent = `${'$'}{seat.price} RUB`;
                      render();
                    });
                    svg.appendChild(circle);
                  });

                  applyTransform();
                }

                svg.addEventListener("pointerdown", (event) => {
                  dragState = { startX: event.clientX, startY: event.clientY, initialTx: translateX, initialTy: translateY };
                  svg.setPointerCapture(event.pointerId);
                });

                svg.addEventListener("pointermove", (event) => {
                  if (!dragState) return;
                  translateX = dragState.initialTx + (event.clientX - dragState.startX);
                  translateY = dragState.initialTy + (event.clientY - dragState.startY);
                  applyTransform();
                });

                svg.addEventListener("pointerup", () => { dragState = null; });
                svg.addEventListener("pointercancel", () => { dragState = null; });
                svg.addEventListener("wheel", (event) => {
                  event.preventDefault();
                  const rect = viewer.getBoundingClientRect();
                  zoomAt(
                    zoom * (event.deltaY < 0 ? 1.1 : 0.9),
                    event.clientX - rect.left,
                    event.clientY - rect.top
                  );
                }, { passive: false });

                viewer.addEventListener("click", () => {
                  selectedSeatId = null;
                  infoTitle.textContent = "ничего";
                  infoMeta.textContent = "Нажми на место";
                  render();
                });
                document.getElementById("fitBtn").addEventListener("click", fitToViewer);
                document.getElementById("zoomInBtn").addEventListener("click", () => { zoomFromViewerCenter(1.2); });
                document.getElementById("zoomOutBtn").addEventListener("click", () => { zoomFromViewerCenter(1 / 1.2); });
                window.addEventListener("resize", fitToViewer);

                fitToViewer();
                render();
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun demoVenue(): Venue {
        val venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        val mainHallId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001")

        return Venue(
            label = "Demo Hall",
            city = City(label = "Ekaterinburg", subject = Subject(label = "Sverdlovsk Oblast")),
            id = venueId,
            spaces = listOf(
                VenueSpace(label = "Main Hall", id = mainHallId),
                VenueSpace(label = "Small Hall")
            )
        )
    }

    private fun demoLayoutTemplate(venueSpaceId: UUID): LayoutTemplate {
        return LayoutTemplate(
            venueSpaceId = venueSpaceId,
            label = "Theatre Layout",
            sections = listOf(
                Section(
                    key = "parter",
                    label = "Партер",
                    rows = listOf(
                        Row(key = "parter-r1", label = "Ряд 1", startSeat = 1, endSeat = 10, price = 2000),
                        Row(key = "parter-r2", label = "Ряд 2", startSeat = 1, endSeat = 10, price = 2000),
                        Row(key = "parter-r3", label = "Ряд 3", startSeat = 1, endSeat = 10, price = 1800)
                    )
                ),
                Section(
                    key = "balkon",
                    label = "Балкон",
                    rows = listOf(
                        Row(key = "balkon-r1", label = "Ряд 1", startSeat = 1, endSeat = 15, price = 1500),
                        Row(key = "balkon-r2", label = "Ряд 2", startSeat = 1, endSeat = 15, price = 1500)
                    )
                )
            )
        )
    }

    private fun demoRenderLayout(): VenueRenderDto {
        return VenueRenderDto(
            schemaVersion = 1,
            stage = StageRenderDto(
                x = 0.22,
                y = 0.04,
                width = 0.56,
                height = 0.08,
                label = "Сцена"
            ),
            sections = listOf(
                SectionRenderDto(
                    sectionKey = "parter",
                    bounds = BoundsDto(x = 0.12, y = 0.18, width = 0.76, height = 0.34),
                    seatLayout = SeatLayoutDto(paddingRatio = 0.10, rowGapRatio = 0.12, seatGapRatio = 0.03)
                ),
                SectionRenderDto(
                    sectionKey = "balkon",
                    bounds = BoundsDto(x = 0.18, y = 0.62, width = 0.64, height = 0.20),
                    seatLayout = SeatLayoutDto(paddingRatio = 0.12, rowGapRatio = 0.18, seatGapRatio = 0.025)
                )
            )
        )
    }
}
