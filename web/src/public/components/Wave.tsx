/** 首页大图底部的波浪动画（来自旧站 WaveComponent.vue） */
export default function Wave() {
  return (
    <div className="wave-wrapper" aria-hidden="true">
      <svg className="waves" viewBox="0 24 150 28" preserveAspectRatio="none" shapeRendering="auto">
        <defs>
          <path id="gentle-wave" d="M-160 44c30 0 58-18 88-18s 58 18 88 18 58-18 88-18 58 18 88 18 v44h-352z" />
        </defs>
        <g className="parallax">
          <use href="#gentle-wave" x="48" y="0" fill="rgba(255,255,255,0.56)" />
          <use href="#gentle-wave" x="48" y="3" fill="rgba(244,244,244,0.72)" />
          <use href="#gentle-wave" x="48" y="5" fill="rgba(241,241,241,0.9)" />
          <use href="#gentle-wave" x="48" y="7" fill="#f2f2f2" />
        </g>
      </svg>
      <div className="wave-fade" />
    </div>
  )
}
