import React from 'react';

const MaterialSelector = ({ materials, selectedMaterial, onSelect }) => {
  return (
    <div className="mb-4">
      <label className="block text-sm font-medium mb-2">Select Material</label>
      <select
        value={selectedMaterial?.id || ''}
        onChange={(e) => {
          const mat = materials.find(m => m.id === parseInt(e.target.value));
          onSelect(mat);
        }}
        className="border p-2 w-full"
      >
        <option value="">Choose material</option>
        {materials.map(material => (
          <option key={material.id} value={material.id}>
            {material.name} ({material.type}) - {material.pricePerSquareMeter} руб/м²
          </option>
        ))}
      </select>
    </div>
  );
};

export default MaterialSelector;