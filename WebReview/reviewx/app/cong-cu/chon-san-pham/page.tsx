"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { EmptyState, LoadingSkeleton, PageContainer, SectionHeader, ShopeeCTAButton } from "@/components/ui";

type QuizStep = 1 | 2 | 3 | 4 | 5;

type BudgetValue = (typeof budgets)[number]["value"];
type CategoryLabel = (typeof categories)[number]["label"];
type PriorityLabel = (typeof priorities)[number];

type ProductCandidate = {
  id: string;
  name: string;
  category: CategoryLabel;
  budget: BudgetValue;
  useCases: string[];
  priorities: PriorityLabel[];
  reason: string;
  pros: string[];
  cons: string[];
  priceLabel: string;
  score: number;
};

const categories = [
  { label: "Công nghệ", icon: "💻" },
  { label: "Gia dụng", icon: "🧺" },
  { label: "Làm đẹp", icon: "💄" },
  { label: "Mẹ & bé", icon: "🍼" },
  { label: "Nhà cửa", icon: "🏠" },
  { label: "Đồ bếp", icon: "🍳" },
  { label: "Gaming", icon: "🎮" },
  { label: "Thể thao", icon: "🏃" },
  { label: "Sách", icon: "📚" },
] as const;

const budgets = [
  { value: "duoi-500", label: "Dưới 500k" },
  { value: "500-1m", label: "500k - 1 triệu" },
  { value: "1-3m", label: "1 - 3 triệu" },
  { value: "3-5m", label: "3 - 5 triệu" },
  { value: "5-10m", label: "5 - 10 triệu" },
  { value: "tren-10m", label: "Trên 10 triệu" },
] as const;

const useCases = ["Dùng hằng ngày", "Đi học", "Đi làm", "Gaming", "Gia đình", "Du lịch"];

const priorities = ["Giá rẻ", "Độ bền", "Thương hiệu", "Hiệu năng", "Thiết kế", "Dễ sử dụng", "Bảo hành tốt", "Nhiều đánh giá tốt"] as const;

const candidates: ProductCandidate[] = [
  {
    id: "sony-wh-ch520",
    name: "Tai nghe Sony WH-CH520",
    category: "Công nghệ",
    budget: "500-1m",
    useCases: ["Dùng hằng ngày", "Đi học", "Đi làm"],
    priorities: ["Độ bền", "Dễ sử dụng", "Thương hiệu"],
    reason: "Pin lâu, đeo nhẹ, phù hợp nhịp dùng hằng ngày.",
    pros: ["Pin dài", "Đeo thoải mái", "Âm thanh cân bằng"],
    cons: ["Không ANC", "Không kháng nước"],
    priceLabel: "790.000đ",
    score: 8.6,
  },
  {
    id: "jbl-go-3",
    name: "Loa Bluetooth JBL Go 3",
    category: "Công nghệ",
    budget: "500-1m",
    useCases: ["Du lịch", "Dùng hằng ngày"],
    priorities: ["Thiết kế", "Dễ sử dụng"],
    reason: "Loa nhỏ gọn, tiện mang theo khi đi chơi hoặc du lịch.",
    pros: ["Nhỏ gọn", "Chống nước", "Thiết kế đẹp"],
    cons: ["Pin chưa quá dài", "Bass ở mức cơ bản"],
    priceLabel: "690.000đ",
    score: 8.2,
  },
  {
    id: "logitech-m331",
    name: "Chuột Logitech M331",
    category: "Công nghệ",
    budget: "duoi-500",
    useCases: ["Đi làm", "Đi học"],
    priorities: ["Độ bền", "Giá rẻ", "Dễ sử dụng"],
    reason: "Bấm êm, pin bền, rất hợp văn phòng và học tập.",
    pros: ["Bấm êm", "Pin bền", "Giá tốt"],
    cons: ["Không Bluetooth", "Ít nút mở rộng"],
    priceLabel: "320.000đ",
    score: 8.1,
  },
  {
    id: "aula-f75",
    name: "Bàn phím cơ Aula F75",
    category: "Gaming",
    budget: "1-3m",
    useCases: ["Gaming", "Đi làm"],
    priorities: ["Hiệu năng", "Thiết kế"],
    reason: "Layout gọn và gõ ổn, phù hợp setup gaming cơ bản.",
    pros: ["Gõ tốt", "Thiết kế đẹp", "Độ hoàn thiện khá"],
    cons: ["Phần mềm còn hạn chế", "Không dành cho người thích layout fullsize"],
    priceLabel: "1.290.000đ",
    score: 7.8,
  },
];

function stepLabel(step: QuizStep) {
  if (step === 1) return "Danh mục";
  if (step === 2) return "Ngân sách";
  if (step === 3) return "Nhu cầu";
  if (step === 4) return "Ưu tiên";
  return "Kết quả";
}

export default function ProductFinderQuizPage() {
  const [step, setStep] = useState<QuizStep>(1);
  const [selectedCategory, setSelectedCategory] = useState("");
  const [selectedBudget, setSelectedBudget] = useState("");
  const [selectedUseCase, setSelectedUseCase] = useState("");
  const [selectedPriorities, setSelectedPriorities] = useState<string[]>([]);
  const [isLoadingResult, setIsLoadingResult] = useState(false);

  const canGoNext =
    (step === 1 && Boolean(selectedCategory)) ||
    (step === 2 && Boolean(selectedBudget)) ||
    (step === 3 && Boolean(selectedUseCase)) ||
    (step === 4 && selectedPriorities.length > 0) ||
    step === 5;

  const results = useMemo(() => {
    const categoryLabels = categories.map((item) => item.label);
    const validCategory = selectedCategory && categoryLabels.includes(selectedCategory as CategoryLabel) ? selectedCategory : "";
    const validBudget = selectedBudget && budgets.some((item) => item.value === selectedBudget) ? selectedBudget : "";
    const validUseCase = selectedUseCase && useCases.includes(selectedUseCase) ? selectedUseCase : "";
    const validPriorities = selectedPriorities.filter((item) => priorities.includes(item as PriorityLabel));

    return candidates
      .filter((item) => {
        if (validCategory && item.category !== validCategory) return false;
        if (validBudget && item.budget !== validBudget) return false;
        if (validUseCase && !item.useCases.includes(validUseCase)) return false;
        if (validPriorities.length > 0 && !validPriorities.some((p) => item.priorities.includes(p as PriorityLabel))) return false;
        return true;
      })
      .map((item) => {
        const matchedPriorities = validPriorities.filter((priority) => item.priorities.includes(priority as PriorityLabel));
        const criteriaScore = [
          validCategory && item.category === validCategory ? 1 : 0,
          validBudget && item.budget === validBudget ? 1 : 0,
          validUseCase && item.useCases.includes(validUseCase) ? 1 : 0,
          validPriorities.length > 0 ? matchedPriorities.length / validPriorities.length : 1,
        ];
        const avg = criteriaScore.reduce((sum, value) => sum + value, 0) / criteriaScore.length;
        const matchPercent = Math.round(avg * 100);
        return {
          ...item,
          matchPercent,
          matchedPriorities,
        };
      })
      .sort((a, b) => b.matchPercent - a.matchPercent || b.score - a.score)
      .slice(0, 3);
  }, [selectedBudget, selectedCategory, selectedPriorities, selectedUseCase]);

  const categoryLabels = categories.map((item) => item.label);
  const hasInvalidSelection =
    (Boolean(selectedCategory) && !categoryLabels.includes(selectedCategory as CategoryLabel)) ||
    (Boolean(selectedBudget) && !budgets.some((item) => item.value === selectedBudget)) ||
    (Boolean(selectedUseCase) && !useCases.includes(selectedUseCase)) ||
    selectedPriorities.some((item) => !priorities.includes(item as PriorityLabel));

  const canShowResults = !hasInvalidSelection;

  function goNext() {
    if (!canGoNext) return;
    if (step === 4) {
      setIsLoadingResult(true);
      setTimeout(() => {
        setIsLoadingResult(false);
        setStep(5);
      }, 350);
      return;
    }
    if (step < 5) setStep((s) => (s + 1) as QuizStep);
  }

  function goBack() {
    if (step > 1) setStep((s) => (s - 1) as QuizStep);
  }

  function resetQuiz() {
    setStep(1);
    setSelectedCategory("");
    setSelectedBudget("");
    setSelectedUseCase("");
    setSelectedPriorities([]);
    setIsLoadingResult(false);
  }

  const totalSteps = 5;
  const currentStepLabel = `Bước ${step}/${totalSteps}`;
  const progressPercent = (step / totalSteps) * 100;

  return (
    <PageContainer>
      <section className="rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-sm sm:p-8">
        <SectionHeader title="Công cụ chọn sản phẩm" subtitle="Trả lời 5 bước để nhận gợi ý phù hợp nhu cầu" />

        <div className="mt-4 space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <p className="text-sm font-semibold text-slate-900">{currentStepLabel}</p>
            <p className="text-xs font-medium text-slate-500">{stepLabel(step)}</p>
          </div>
          <div className="h-2 w-full overflow-hidden rounded-full bg-slate-200">
            <div className="h-full rounded-full bg-blue-600 transition-all duration-300" style={{ width: `${progressPercent}%` }} />
          </div>
          <div className="flex flex-wrap gap-2">
            {[1, 2, 3, 4, 5].map((value) => {
              const current = value as QuizStep;
              const active = step === current;
              const completed = step > current;
              return (
                <div key={value} className={`rounded-full border px-3 py-1.5 text-xs font-semibold ${active ? "border-blue-200 bg-blue-50 text-blue-700" : completed ? "border-emerald-200 bg-emerald-50 text-emerald-700" : "border-slate-200 bg-white text-slate-600"}`}>
                  Bước {value}: {stepLabel(current)}
                </div>
              );
            })}
          </div>
        </div>

        <div className="mt-5 rounded-2xl border border-slate-200 bg-slate-50 p-4 transition-all duration-300 ease-out data-[step-changing=true]:translate-y-1 data-[step-changing=true]:opacity-70" data-step-changing={isLoadingResult && step === 4}>
          {step === 1 ? (
            <div className="space-y-3">
              <p className="text-sm font-semibold text-slate-900">Chọn danh mục</p>
              <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                {categories.map((item) => {
                  const active = selectedCategory === item.label;
                  return (
                    <button
                      key={item.label}
                      type="button"
                      onClick={() => setSelectedCategory(item.label)}
                      className={`min-h-12 rounded-2xl border px-3 py-3 text-left text-sm font-medium transition hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 ${active ? "border-blue-200 bg-blue-50 text-blue-700" : "border-slate-200 bg-white text-slate-700"}`}
                    >
                      <span className="flex items-center justify-between gap-2">
                        <span className="inline-flex items-center gap-2">
                          <span>{item.icon}</span>
                          <span>{item.label}</span>
                        </span>
                        {active ? <span className="text-xs">✓</span> : null}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          ) : null}

          {step === 2 ? (
            <div className="space-y-3">
              <p className="text-sm font-semibold text-slate-900">Chọn ngân sách</p>
              <div className="flex flex-wrap gap-2">
                {budgets.map((item) => {
                  const active = selectedBudget === item.value;
                  return (
                    <button
                      key={item.value}
                      type="button"
                      onClick={() => setSelectedBudget(item.value)}
                      className={`min-h-11 rounded-full border px-4 py-2 text-sm font-semibold transition hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 ${active ? "border-blue-200 bg-blue-50 text-blue-700" : "border-slate-200 bg-white text-slate-700"}`}
                    >
                      <span className="inline-flex items-center gap-2">
                        <span>{item.label}</span>
                        {active ? <span className="text-xs">✓</span> : null}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          ) : null}

          {step === 3 ? (
            <div className="space-y-3">
              <p className="text-sm font-semibold text-slate-900">Chọn nhu cầu sử dụng</p>
              <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                {useCases.map((item) => (
                  <button key={item} type="button" onClick={() => setSelectedUseCase(item)} className={`rounded-2xl border px-3 py-3 text-left text-sm font-medium ${selectedUseCase === item ? "border-blue-200 bg-blue-50 text-blue-700" : "border-slate-200 bg-white text-slate-700"}`}>
                    {item}
                  </button>
                ))}
              </div>
            </div>
          ) : null}

          {step === 4 ? (
            <div className="space-y-3">
              <p className="text-sm font-semibold text-slate-900">Chọn ưu tiên chính</p>
              <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                {priorities.map((item) => {
                  const active = selectedPriorities.includes(item);
                  return (
                    <button
                      key={item}
                      type="button"
                      onClick={() => setSelectedPriorities((prev) => (prev.includes(item) ? prev.filter((x) => x !== item) : [...prev, item]))}
                      className={`min-h-12 rounded-2xl border px-3 py-3 text-left text-sm font-medium transition hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 ${active ? "border-blue-200 bg-blue-50 text-blue-700" : "border-slate-200 bg-white text-slate-700"}`}
                    >
                      <span className="flex items-center justify-between gap-2">
                        <span>{item}</span>
                        {active ? <span className="text-xs">✓</span> : null}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          ) : null}

          {step === 5 ? (
            <div className="space-y-3">
              <p className="text-sm font-semibold text-slate-900">Kết quả gợi ý</p>
              {isLoadingResult ? (
                <div className="grid gap-3 sm:grid-cols-2">
                  <LoadingSkeleton className="h-44 w-full rounded-2xl" />
                  <LoadingSkeleton className="h-44 w-full rounded-2xl" />
                </div>
              ) : !canShowResults ? (
                <EmptyState title="Lỗi dữ liệu bộ lọc" message="Tiêu chí hiện tại không hợp lệ. Vui lòng reset quiz để thử lại." />
              ) : results.length === 0 ? (
                <EmptyState title="Không có sản phẩm phù hợp" message="Hãy reset quiz hoặc đổi tiêu chí để nhận gợi ý khác." />
              ) : (
                <>
                  <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                    {results.map((item) => (
                      <article key={item.id} className="rounded-2xl border border-slate-200/70 bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md">
                        <div className="flex items-center justify-between gap-2">
                          <h3 className="text-sm font-semibold text-slate-900">{item.name}</h3>
                          <span className="rounded-full border border-emerald-200 bg-emerald-50 px-2 py-0.5 text-xs font-semibold text-emerald-700">{item.matchPercent}%</span>
                        </div>
                        <p className="mt-1 text-xs text-slate-500">{item.priceLabel} · Điểm {item.score}</p>
                        <p className="mt-2 text-sm text-slate-700">{item.reason}</p>
                        <p className="mt-2 text-xs text-slate-600"><span className="font-semibold">Ưu điểm:</span> {item.pros.join(", ")}</p>
                        <p className="mt-1 text-xs text-slate-600"><span className="font-semibold">Nhược điểm:</span> {item.cons.join(", ")}</p>
                        <div className="mt-3 flex flex-wrap gap-2">
                          <Link href={`/review/${item.id}`} className="inline-flex items-center rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700">Xem review</Link>
                          <ShopeeCTAButton href={`/go/product/${item.id}`}>Xem giá</ShopeeCTAButton>
                        </div>
                      </article>
                    ))}
                  </div>
                  <div className="rounded-2xl border border-slate-200/70 bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md">
                    <p className="text-sm font-semibold text-slate-900">So sánh nhanh 3 gợi ý này</p>
                    <p className="mt-1 text-xs text-slate-500">Mở trang so sánh với các sản phẩm đã chọn sẵn.</p>
                    <Link
                      href={`/so-sanh?ids=${results.map((item) => item.id).join(",")}`}
                      className="mt-3 inline-flex items-center rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-blue-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
                    >
                      So sánh các sản phẩm này
                    </Link>
                  </div>
                </>
              )}
            </div>
          ) : null}
        </div>

        <div className="mt-4 flex flex-wrap gap-2">
          <button type="button" onClick={goBack} disabled={step === 1} className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 disabled:cursor-not-allowed disabled:opacity-50">
            Quay lại
          </button>
          <button
            type="button"
            onClick={goNext}
            disabled={!canGoNext || step === 5}
            className="rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50"
          >
            Tiếp tục
          </button>
          <button type="button" onClick={resetQuiz} className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700">
            Reset quiz
          </button>
        </div>
      </section>
    </PageContainer>
  );
}
