import React from 'react';

const DimensionsInput = ({ dimensions, onChange }) => {
  const handleChange = (field, value) => {
    onChange({ ...dimensions, [field]: value });
  };

  return (
    <div className="mb-4">
      <label className="block text-sm font-medium mb-2">Dimensions (meters)</label>
      <div className="flex space-x-2">
        <input
          type="number"
          step="0.01"
          placeholder="Width (m)"
          value={dimensions.widthM}
          onChange={(e) => handleChange('widthM', e.target.value)}
          className="border p-2 flex-1"
        />
        <input
          type="number"
          step="0.01"
          placeholder="Height (m)"
          value={dimensions.heightM}
          onChange={(e) => handleChange('heightM', e.target.value)}
          className="border p-2 flex-1"
        />
      </div>
    </div>
  );
};

export default DimensionsInput;