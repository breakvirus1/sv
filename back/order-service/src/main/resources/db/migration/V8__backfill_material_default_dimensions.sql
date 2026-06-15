-- Data migration V8: fill default_width_mm and default_height_mm for existing materials.
-- Columns were added in V7 as NOT NULL DEFAULT 0, so all pre-existing rows have 0 here.
-- These values represent standard product dimensions (in mm) used by the frontend to
-- pre-fill position width/height when adding a new item to an order.

-- Banner by running metre: width = standard roll width, height = 0 (cut by length)
--   widthMm = roll width in mm (e.g. 1600 mm for a 1.6 m roll)
--   heightMm = 0 for м.п. items — не показывается в форме.
-- Sheet material (м2): both width and height are populated so that %
--   effective quantity  = (width / 1000) * (height / 1000) %
--   user can immediately see the area pre-filled.

UPDATE ordschema.materials
SET  default_width_mm  = CASE id
        WHEN 1  THEN 1600   -- Баннер Frontlit 440 г/м²  — рулон 1.60 м
        WHEN 2  THEN 1070   -- Баннер Blockout 510 г/м² — рулон 1.07 м
        WHEN 3  THEN 1370   -- Плёнка monomer 80 мкм    — рулон 1.37 м
        WHEN 4  THEN 1370   -- Плёнка cast 100 мкм      — рулон 1.37 м
        ELSE default_width_mm
    END,
     default_height_mm = CASE id
         WHEN 1  THEN 0      -- м.п.: не используется
         WHEN 2  THEN 0      -- м.п.: не используется
         WHEN 3  THEN 0      -- м.п.: не используется
         WHEN 4  THEN 0      -- м.п.: не используется
         ELSE default_height_mm
     END
WHERE id IN (1, 2, 3, 4);
