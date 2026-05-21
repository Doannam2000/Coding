"use client";

import { useState, useCallback, useEffect } from "react";
import Image from "next/image";
import Link from "next/link";
import { ChevronLeft, ChevronRight, X } from "lucide-react";

type ProductGalleryProps = {
  slug: string;
  name: string;
  isHot: boolean;
  images: string[];
  activeImage: string;
};

export function ProductGallery({ slug, name, isHot, images, activeImage }: ProductGalleryProps) {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalIndex, setModalIndex] = useState(0);

  const openModal = useCallback((index: number) => {
    setModalIndex(index);
    setIsModalOpen(true);
  }, []);

  const closeModal = useCallback(() => {
    setIsModalOpen(false);
  }, []);

  const goPrev = useCallback(() => {
    setModalIndex((prev) => (prev - 1 + images.length) % images.length);
  }, [images.length]);

  const goNext = useCallback(() => {
    setModalIndex((prev) => (prev + 1) % images.length);
  }, [images.length]);

  useEffect(() => {
    if (!isModalOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") closeModal();
      if (e.key === "ArrowLeft") goPrev();
      if (e.key === "ArrowRight") goNext();
    };

    window.addEventListener("keydown", handleKeyDown);
    document.body.style.overflow = "hidden";

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = "";
    };
  }, [isModalOpen, goPrev, goNext, closeModal]);

  return (
    <div className="space-y-4">
      {/* Main image */}
      <div className="relative overflow-hidden rounded-3xl border border-slate-200 bg-slate-100 cursor-pointer" onClick={() => openModal(0)}>
        <Image src={activeImage} alt={name} width={1200} height={900} className="h-72 w-full object-cover sm:h-96" />
        {isHot && <span className="absolute left-3 top-3 rounded-full bg-red-500 px-3 py-1 text-xs font-semibold text-white">Hot</span>}
        <div className="absolute inset-0 flex items-center justify-center bg-black/0 transition hover:bg-black/20">
          <span className="text-white/0 transition group-hover:text-white/80 text-sm font-medium">Xem ảnh lớn</span>
        </div>
      </div>

      {/* Thumbnails */}
      <div className="grid grid-cols-3 gap-3 sm:grid-cols-4">
        {images.map((item, index) => (
          <Link
            key={`${slug}-${index}`}
            href={`/san-pham/${slug}?image=${index}`}
            className={`relative overflow-hidden rounded-2xl border bg-slate-100 ${activeImage === item ? "border-blue-400" : "border-slate-200"}`}
          >
            <Image src={item} alt={`${name} ${index + 1}`} width={400} height={300} className="h-20 w-full object-cover" />
          </Link>
        ))}
      </div>

      {/* Preview Modal */}
      {isModalOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4"
          onClick={closeModal}
          role="dialog"
          aria-label="Xem ảnh lớn"
        >
          <button
            onClick={closeModal}
            className="absolute right-4 top-4 rounded-full bg-white/10 p-2 text-white transition hover:bg-white/20"
            aria-label="Đóng"
          >
            <X className="h-6 w-6" />
          </button>

          <button
            onClick={goPrev}
            className="absolute left-4 top-1/2 -translate-y-1/2 rounded-full bg-white/10 p-3 text-white transition hover:bg-white/20"
            aria-label="Ảnh trước"
          >
            <ChevronLeft className="h-6 w-6" />
          </button>

          <button
            onClick={goNext}
            className="absolute right-4 top-1/2 -translate-y-1/2 rounded-full bg-white/10 p-3 text-white transition hover:bg-white/20"
            aria-label="Ảnh sau"
          >
            <ChevronRight className="h-6 w-6" />
          </button>

          <div className="relative max-h-[85vh] max-w-[90vw]">
            <Image
              src={images[modalIndex]}
              alt={`${name} - ${modalIndex + 1}`}
              width={1200}
              height={900}
              className="max-h-[85vh] w-auto rounded-2xl object-contain"
            />
            <p className="text-center text-sm text-white/70 mt-2">
              {modalIndex + 1} / {images.length}
            </p>
          </div>
        </div>
      )}
    </div>
  );
}